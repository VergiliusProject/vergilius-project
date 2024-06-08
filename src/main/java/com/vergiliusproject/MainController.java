package com.vergiliusproject;

import com.vergiliusproject.dto.NamedSlug;
import com.vergiliusproject.dto.Root;
import com.vergiliusproject.dto.TypeEntry;
import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Tdata;
import com.vergiliusproject.entities.Ttype;
import com.vergiliusproject.repos.OsRepository;
import com.vergiliusproject.repos.TdataRepository;
import com.vergiliusproject.repos.TtypeRepository;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

@Controller
public class MainController implements ErrorController {
    @Value("${GIT_HASH}")
    private String gitHash;
    @Autowired
    public OsRepository osRepo;
    @Autowired
    public TtypeRepository ttypeRepo;
    @Autowired
    public TdataRepository tdataRepo;   

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("mes", "404");
            } else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("mes", "505");
            }
        }

        addCommonAttributes(model);
        return "404";
    }

    @GetMapping("/admin")
    public String displayAdmin(Model model) throws IOException {
        addCommonAttributes(model);
        return "admin";
    }

    @PostMapping("/admin")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try (InputStream res = file.getInputStream()) {
            Yaml yaml = new Yaml();
            yaml.setBeanAccess(BeanAccess.FIELD);
            Root root = yaml.loadAs(res, Root.class);

            Os os = new Os();

            os.setFamilyName(root.getFamily());
            os.setOsName(root.getOsname());
            os.setOldFamilyName(root.getOldfamily());
            os.setOldOsName(root.getOldosname());
            os.setBuildnumber(root.getBuildnumber());
            os.setArch(root.getArch());
            os.setTimestamp(root.getTimestamp());

            List<Ttype> types = root.getTypes();
            
            types.stream().forEach(type -> { 
                type.setOpersys(os); 
                Set<Tdata> datas = type.getData();
                
                if (datas != null) {
                    datas.forEach(data -> data.setTtype(type));
                }
            });
            
            os.setTtypes(new HashSet<>(types));
            osRepo.save(os);
        }
        catch (IOException e){}

        redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/admin";
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("x86families", osRepo.findByArch("x86").stream().sorted(Comparator.reverseOrder())
                .map(x -> new NamedSlug(x.getFamilyName(), x.getFamilySlug())).distinct().toList());
        model.addAttribute("x64families", osRepo.findByArch("x64").stream().sorted(Comparator.reverseOrder())
                .map(x -> new NamedSlug(x.getFamilyName(), x.getFamilySlug())).distinct().toList());
        model.addAttribute("gitHash", gitHash);
        model.addAttribute("canonicalUrl", ServletUriComponentsBuilder.fromCurrentRequestUri().host("www.vergiliusproject.com").scheme("https").port("").build());
    }
    
    private Optional<Os> getPreviousOs(Os os) {
        return osRepo.findByArch(os.getArch()).stream().sorted(Comparator.reverseOrder()).filter(x -> x.compareTo(os) < 0).findFirst();
    }
    
    private Optional<Os> getNextOs(Os os) {
        return osRepo.findByArch(os.getArch()).stream().sorted().filter(x -> x.compareTo(os) > 0).findFirst();
    }

    @GetMapping("/")
    public String displayHome(Model model) {
        addCommonAttributes(model);
        return "home";
    }

    @GetMapping("/terms")
    public String displayTerms(Model model) {
        addCommonAttributes(model);
        return "terms";
    }

    @GetMapping("/privacy")
    public String displayPrivacy(Model model) {
        addCommonAttributes(model);
        return "privacy";
    }

    @GetMapping("/about")
    public String displayAbout(Model model) {
        addCommonAttributes(model);
        return "about";
    }

    @GetMapping("/kernels")
    public String displayKernels(Model model) throws Exception {
        addCommonAttributes(model);
        return "kernels";
    }

    @GetMapping("/kernels/{arch:.+}")
    public String displayArch(@PathVariable String arch, Model model) {
        model.addAttribute("families", osRepo.findByArch(arch).stream().sorted(Comparator.reverseOrder())
                .map(x -> new NamedSlug(x.getFamilyName(), x.getFamilySlug())).distinct().toList());

        addCommonAttributes(model);
        return "arch";
    }

    @RequestMapping(value="/kernels/{arch:.+}/{familySlug:.+}")
    public String displayFamily(@PathVariable String arch, @PathVariable String familySlug, Model model) {
        List<Os> oses = osRepo.findByArchAndFamilySlug(arch, familySlug);
        
        // deal with old family names
        if (oses.isEmpty()) {
            oses = osRepo.findByArchAndOldFamilyName(arch, familySlug);
            
            if (oses.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            
            // redirect to a new url: oldFamilyName -> familySlug
            model.addAttribute("targetUrl", "/kernels/" + arch + "/" + oses.getFirst().getFamilySlug());
            return "redirect";
        }
        
        Collections.sort(oses, Collections.reverseOrder());
                
        model.addAttribute("oses", oses);
        model.addAttribute("family", new NamedSlug(oses.getFirst().getFamilyName(), oses.getFirst().getFamilySlug()));

        addCommonAttributes(model);
        return "family";
    }
    
    private List<TypeEntry> makeTypeEnties(List<Ttype> types, Set<String> prevTypes) {
        return types.stream().map(x -> new TypeEntry(x.getName(), prevTypes.isEmpty() ? false : !prevTypes.contains(x.getName()))).toList();
    }

    @RequestMapping(value = "/kernels/{arch:.+}/{familySlug:.+}/{osSlug:.+}", method = RequestMethod.GET)
    public String displayKinds(@PathVariable String arch, @PathVariable String familySlug, @PathVariable String osSlug, Model model) {
        Os os = osRepo.findByArchAndFamilySlugAndOsSlug(arch, familySlug, osSlug);
        
        // deal with old family names
        if (os == null) {
            os = osRepo.findByArchAndOldFamilyNameAndOldOsName(arch, familySlug, osSlug);
            
            if (os == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            
            // redirect to a new url: oldFamilyName -> familySlug, oldOsName -> osSlug
            model.addAttribute("targetUrl", "/kernels/" + arch + "/" + os.getFamilySlug() + "/" + os.getOsSlug());            
            return "redirect";
        }
        
        List<Ttype> types = ttypeRepo.findByOpersysAndIsConstFalseAndIsVolatileFalse(os);
        
        Set<String> prevTypes = Collections.emptySet();

        Optional<Os> prevOs = getPreviousOs(os);
        if (prevOs.isPresent()) {
            prevTypes = ttypeRepo.findByOpersysAndIsConstFalseAndIsVolatileFalse(prevOs.get()).stream().map(Ttype::getName).collect(Collectors.toSet());
        }        

        model.addAttribute("structs", makeTypeEnties(Sorter.sortByName(Ttype.filterByTypes(types, Ttype.Kind.STRUCT)), prevTypes));
        model.addAttribute("unions", makeTypeEnties(Sorter.sortByName(Ttype.filterByTypes(types, Ttype.Kind.UNION)), prevTypes));
        model.addAttribute("enums", makeTypeEnties(Sorter.sortByName(Ttype.filterByTypes(types, Ttype.Kind.ENUM)), prevTypes));
        
        model.addAttribute("family", new NamedSlug(os.getFamilyName(), os.getFamilySlug()));
        model.addAttribute("os", new NamedSlug(os.getOsName(), os.getOsSlug()));

        addCommonAttributes(model);
        return "ttype";
    }

    @RequestMapping(value = "/kernels/{arch:.+}/{familySlug:.+}/{osSlug:.+}/{name:.+}", method = RequestMethod.GET)
    public String displayType(@PathVariable String arch, @PathVariable String familySlug, @PathVariable String osSlug, @PathVariable String name, Model model) {
        Os os = osRepo.findByArchAndFamilySlugAndOsSlug(arch, familySlug, osSlug);
        
        // deal with old family names
        if (os == null) {
            os = osRepo.findByArchAndOldFamilyNameAndOldOsName(arch, familySlug, osSlug);
            
            if (os == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            
            // redirect to a new url: oldFamilyName -> familySlug, oldOsName -> osSlug
            model.addAttribute("targetUrl", "/kernels/" + arch + "/" + os.getFamilySlug() + "/" + os.getOsSlug() + "/" + name);
            return "redirect";
        }

        model.addAttribute("ttypename", name);

        List<Ttype> typeslist = ttypeRepo.findByNameAndOpersys(name, os);

        if (typeslist != null && !typeslist.isEmpty()) {                         
            try {
                String link = "/kernels/" + arch + "/" + familySlug + "/" + osSlug + "/";
                model.addAttribute("ttype", FieldBuilder.recursionProcessing(ttypeRepo, typeslist.getFirst(), 0, 0, link, os).toString());
            }
            catch (Exception e) {
                System.out.println(e.getClass());
                for(StackTraceElement each: e.getStackTrace())
                {
                    System.out.println(each);
                }
            }
            
            //search for cross-links
            List<Ttype> used_in = new ArrayList<>();

            for (Ttype i : typeslist) {
                used_in.addAll(ttypeRepo.findByOpersysAndId1(os, i.getId()));
                used_in.addAll(ttypeRepo.findByOpersysAndId2(os, i.getId()));
                used_in.addAll(ttypeRepo.findByOpersysAndId3(os, i.getId()));
                used_in.addAll(ttypeRepo.findByOpersysAndId4(os, i.getId()));
                used_in.addAll(ttypeRepo.findByOpersysAndId5(os, i.getId()));
            }

            List<String> used_in_names = used_in.stream().map(Ttype::getName).sorted().distinct().toList();
                    
            model.addAttribute("cros", used_in_names.isEmpty() ? null : used_in_names);
        }

        model.addAttribute("family", new NamedSlug(os.getFamilyName(), os.getFamilySlug()));
        model.addAttribute("os", new NamedSlug(os.getOsName(), os.getOsSlug()));
        
        getPreviousOs(os).ifPresent(x -> model.addAttribute("previousOs", x));
        getNextOs(os).ifPresent(x -> model.addAttribute("nextOs", x));
        
        addCommonAttributes(model);
        return "tdata";
    }
    
    @GetMapping("/oldlinks")
    public String displayOldLinks(Model model) throws IOException {
        List<Os> oses = osRepo.findByOldFamilyNameNotNull();
        
        List<String> families = oses.stream()
            .map(os -> "/kernels/" + os.getArch() + "/" + os.getOldFamilyName())
            .distinct()
            .toList();
        List<String> osnames = oses.stream()
            .map(os -> "/kernels/" + os.getArch() + "/" + os.getOldFamilyName() + "/" + os.getOldOsName())
            .toList();
        List<String> ttypes = oses.stream()
            .flatMap(os -> ttypeRepo.findStructEnumUnionByOpersys(os).stream()
                .map(ttype -> "/kernels/" + os.getArch() + "/" + os.getOldFamilyName() + "/" + os.getOldOsName() + "/" + ttype.getName()))
            .toList();
        
        model.addAttribute("families", families);
        model.addAttribute("osnames", osnames);
        model.addAttribute("ttypes", ttypes);

        return "oldlinks";
    }
}