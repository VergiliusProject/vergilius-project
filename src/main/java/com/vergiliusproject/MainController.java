package com.vergiliusproject;

import com.vergiliusproject.dto.Root;
import com.vergiliusproject.dto.TypeEntry;
import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Tdata;
import com.vergiliusproject.entities.Ttype;
import com.vergiliusproject.repos.OsRepository;
import com.vergiliusproject.repos.TdataRepository;
import com.vergiliusproject.repos.TtypeRepository;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

@Controller
public class MainController implements ErrorController {
    @Autowired
    public OsRepository osRepo;
    @Autowired
    public TtypeRepository ttypeRepo;
    @Autowired
    public TdataRepository tdataRepo;

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        passFamilyList(model);

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("mes", "404");
            } else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("mes", "505");
            }
        }

        return "404";
    }

    @GetMapping("/admin")
    public String displayAdmin(Model model) throws IOException {
        passFamilyList(model);

        return "admin";
    }

    @PostMapping("/admin")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try (InputStream res = file.getInputStream()) {
            Yaml yaml = new Yaml();
            yaml.setBeanAccess(BeanAccess.FIELD);
            Root root = yaml.loadAs(res, Root.class);

            Os os = new Os();

            os.setFamily(root.getFamily());
            os.setOsname(root.getOsname());
            os.setOldfamily(root.getOldfamily());
            os.setOldosname(root.getOldosname());
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

    private void passFamilyList(Model model) {
        model.addAttribute("sortedFamx86", Sorter.sortByBuildnumber(osRepo.findOsByArch("x86"), false).stream().map(Os::getFamily).distinct().toList());
        model.addAttribute("sortedFamx64", Sorter.sortByBuildnumber(osRepo.findOsByArch("x64"), false).stream().map(Os::getFamily).distinct().toList());
    }
    
    private Optional<Os> getPreviousOs(Os os) {
        return Sorter.sortByBuildnumber(osRepo.findOsByArch(os.getArch()), false).stream().filter(x -> x.compare(x, os) < 0).findFirst();
    }

    @GetMapping("/")
    public String displayHome(Model model) {
        passFamilyList(model);
        return "home";
    }

    @GetMapping("/terms")
    public String displayTerms(Model model) {
        passFamilyList(model);
        return "terms";
    }

    @GetMapping("/privacy")
    public String displayPrivacy(Model model) {
        passFamilyList(model);
        return "privacy";
    }

    @GetMapping("/about")
    public String displayAbout(Model model) {
        passFamilyList(model);
        return "about";
    }

    @GetMapping("/kernels")
    public String displayKernels(Model model) throws Exception {
        passFamilyList(model);
        return "kernels";
    }

    @GetMapping("/kernels/{arch:.+}")
    public String displayArch(@PathVariable String arch, Model model) {
        model.addAttribute("chosenArch", Sorter.sortByBuildnumber(osRepo.findOsByArch(arch), false).stream().map(os -> os.getFamily()).distinct().toList());

        passFamilyList(model);

        return "arch";
    }

    @RequestMapping(value="/kernels/{arch:.+}/{famname:.+}")
    public String displayFamily(@PathVariable String arch, @PathVariable String famname, Model model) {
        List<Os> oses = osRepo.findByArchAndFamily(arch, famname);
        
        // deal with the old family name
        if (oses.isEmpty()) {
            oses = osRepo.findByArchAndOldFamily(arch, famname);
            
            if (oses.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            
            model.addAttribute("targetUrl", "/kernels/" + arch + "/" + oses.getFirst().getFamily());
            return "redirect";
        }
        
        model.addAttribute("fam", Sorter.sortByBuildnumber(oses, false));

        passFamilyList(model);

        return "family";
    }
    
    private List<TypeEntry> makeTypeEnties(List<Ttype> types, Set<String> prevTypes) {
        return types.stream().map(x -> new TypeEntry(x.getName(), prevTypes.isEmpty() ? false : !prevTypes.contains(x.getName()))).toList();
    }

    @RequestMapping(value = "/kernels/{arch:.+}/{famname:.+}/{osname:.+}", method = RequestMethod.GET)
    public String displayKinds(@PathVariable String arch, @PathVariable String famname, @PathVariable String osname, Model model) {
        Os os = osRepo.findByArchAndFamilyAndOsname(arch, famname, osname);
        
        // deal with the old family name
        if (os == null) {
            os = osRepo.findByArchAndOldFamilyAndOldOsname(arch, famname, osname);
            
            if (os == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            
            model.addAttribute("targetUrl", "/kernels/" + arch + "/" + os.getFamily() + "/" + os.getOsname());
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

        passFamilyList(model);

        return "ttype";
    }

    @RequestMapping(value = "/kernels/{arch:.+}/{famname:.+}/{osname:.+}/{name:.+}", method = RequestMethod.GET)
    public String displayType(@PathVariable String arch, @PathVariable String famname, @PathVariable String osname, @PathVariable String name, Model model) {
        Os opersys = osRepo.findByArchAndFamilyAndOsname(arch, famname, osname);
        
        // deal with the old family name
        if (opersys == null) {
            opersys = osRepo.findByArchAndOldFamilyAndOldOsname(arch, famname, osname);
            
            if (opersys == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            
            model.addAttribute("targetUrl", "/kernels/" + arch + "/" + opersys.getFamily() + "/" + opersys.getOsname() + "/" + name);
            return "redirect";
        }

        String link = "/kernels/" + arch + "/" + famname + "/" + osname + "/";

        List<Ttype> typeslist = ttypeRepo.findByNameAndOpersys(name, opersys);

        if (typeslist != null && !typeslist.isEmpty()) {
            model.addAttribute("ttypename", typeslist.get(0).getName());
             
            try {
                model.addAttribute("ttype", FieldBuilder.recursionProcessing(ttypeRepo, typeslist.get(0), 0, 0, link, opersys).toString());

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
                used_in.addAll(ttypeRepo.findByOpersysAndId1(opersys, i.getId()));
                used_in.addAll(ttypeRepo.findByOpersysAndId2(opersys, i.getId()));
                used_in.addAll(ttypeRepo.findByOpersysAndId3(opersys, i.getId()));
                used_in.addAll(ttypeRepo.findByOpersysAndId4(opersys, i.getId()));
                used_in.addAll(ttypeRepo.findByOpersysAndId5(opersys, i.getId()));
            }

            List<String> used_in_names = used_in.stream().map(Ttype::getName).sorted().distinct().toList();
                    
            model.addAttribute("cros", used_in_names.isEmpty() ? null : used_in_names);
        }

        model.addAttribute("currOs", opersys);

        List<Os> os = Sorter.sortByBuildnumber(osRepo.findOsByArch(opersys.getArch()), true);
        Map<Os, Integer> map = new LinkedHashMap<>();
        Map<Integer, Os> mapInverted = new LinkedHashMap<>();
        for (int i = 1; i <= os.size(); i++) {
            map.put(os.get(i - 1), i);
            mapInverted.put(i, os.get(i - 1));
        }

        model.addAttribute("mapos", map);
        model.addAttribute("invertMapos", mapInverted);

        passFamilyList(model);

        return "tdata";
    }
    
    @GetMapping("/oldlinks")
    public String displayOldLinks(Model model) throws IOException {
        List<Os> oses = osRepo.findWithOldFamilyNotNull();
        
        List<String> families = oses.stream()
            .map(os -> "/kernels/" + os.getArch() + "/" + os.getOldfamily())
            .distinct()
            .toList();
        List<String> osnames = oses.stream()
            .map(os -> "/kernels/" + os.getArch() + "/" + os.getOldfamily() + "/" + os.getOldosname())
            .toList();
        List<String> ttypes = oses.stream()
            .flatMap(os -> ttypeRepo.findStructEnumUnionByOpersys(os).stream()
                .map(ttype -> "/kernels/" + os.getArch() + "/" + os.getOldfamily() + "/" + os.getOldosname() + "/" + ttype.getName()))
            .toList();
        
        model.addAttribute("families", families);
        model.addAttribute("osnames", osnames);
        model.addAttribute("ttypes", ttypes);

        return "oldlinks";
    }
}