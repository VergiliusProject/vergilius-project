package com.vergiliusproject;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.yaml.snakeyaml.Yaml;

import org.yaml.snakeyaml.introspector.BeanAccess;
import com.vergiliusproject.repos.TdataRepository;
import com.vergiliusproject.repos.OsRepository;
import com.vergiliusproject.repos.TtypeRepository;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import com.vergiliusproject.dto.TypeEntry;
import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Tdata;
import com.vergiliusproject.entities.Ttype;

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

            os.setOsname(root.getOsname());
            os.setFamily(root.getFamily());
            os.setTimestamp(root.getTimestamp());
            os.setBuildnumber(root.getBuildnumber());
            os.setArch(root.getArch());

            List<Ttype> types = root.getTypes();

            for (Ttype type: types) {
                type.setOpersys(os);
                Set<Tdata> datas = type.getData();

                if (datas != null) {
                    for (Tdata data: datas) {
                        data.setTtype(type);
                    }
                }
            }
            
            os.setTypes(new HashSet<>(types));
            osRepo.save(os);
        }
        catch (IOException e){}

        redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/admin";
    }

    private void passFamilyList(Model model) {
        model.addAttribute("sortedFamx86", Sorter.sortByBuildnumber(osRepo.findOsByArch("x86"), false).stream().map(os -> os.getFamily()).distinct().collect(Collectors.toList()));
        model.addAttribute("sortedFamx64", Sorter.sortByBuildnumber(osRepo.findOsByArch("x64"), false).stream().map(os -> os.getFamily()).distinct().collect(Collectors.toList()));
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
        model.addAttribute("chosenArch", Sorter.sortByBuildnumber(osRepo.findOsByArch(arch), false).stream().map(os -> os.getFamily()).distinct().collect(Collectors.toList()));

        passFamilyList(model);

        return "arch";
    }

    @RequestMapping(value="/kernels/{arch:.+}/{famname:.+}")
    public String displayFamily(@PathVariable String arch, @PathVariable String famname, Model model) {
        model.addAttribute("fam", Sorter.sortByBuildnumber(osRepo.findByArchAndFamily(arch, famname), false));

        passFamilyList(model);

        return "family";
    }
    
    private List<TypeEntry> makeTypeEnties(List<Ttype> types, Set<String> prevTypes) {
        return types.stream().map(x -> new TypeEntry(x.getName(), prevTypes.isEmpty() ? false : !prevTypes.contains(x.getName()))).collect(Collectors.toList());
    }

    @RequestMapping(value = "/kernels/{arch:.+}/{famname:.+}/{osname:.+}", method = RequestMethod.GET)
    public String displayKinds(@PathVariable String arch, @PathVariable String famname, @PathVariable String osname, Model model) {
        Os os = osRepo.findByArchAndFamilyAndOsname(arch, famname, osname);
        List<Ttype> types = ttypeRepo.findByOpersysAndIsConstFalseAndIsVolatileFalse(os);
        
        Set<String> prevTypes = Collections.emptySet();

        Optional<Os> prevOs = getPreviousOs(os);
        if (prevOs.isPresent()) {
            prevTypes = ttypeRepo.findByOpersysAndIsConstFalseAndIsVolatileFalse(prevOs.get()).stream().map(x -> x.getName()).collect(Collectors.toSet());
        }        

        model.addAttribute("structs", makeTypeEnties(Sorter.sortByName(Ttype.FilterByTypes(types, Ttype.Kind.STRUCT)), prevTypes));
        model.addAttribute("unions", makeTypeEnties(Sorter.sortByName(Ttype.FilterByTypes(types, Ttype.Kind.UNION)), prevTypes));
        model.addAttribute("enums", makeTypeEnties(Sorter.sortByName(Ttype.FilterByTypes(types, Ttype.Kind.ENUM)), prevTypes));

        passFamilyList(model);

        return "ttype";
    }

    @RequestMapping(value = "/kernels/{arch:.+}/{famname:.+}/{osname:.+}/{name:.+}", method = RequestMethod.GET)
    public String displayType(@PathVariable String arch, @PathVariable String famname,@PathVariable String osname,@PathVariable String name, Model model) {
        Os opersys = osRepo.findByArchAndFamilyAndOsname(arch, famname, osname);

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

            List<String> used_in_names = new ArrayList<>();
            for (Ttype i : used_in) {
                used_in_names.add(i.getName());
            }

            if (!used_in_names.isEmpty()) {
                used_in_names = used_in_names.stream().distinct().sorted().collect(Collectors.toList());
            } else {
                used_in_names = null;
            }
            model.addAttribute("cros", used_in_names);
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
    
    @GetMapping("/redirect")
    public String displayRedirect(Model model) throws IOException {
        model.addAttribute("targetUrl", "/about");

        return "redirect";
    }
    
    @GetMapping("/oldlinks")
    public String displayOldLinks(Model model) throws IOException {
        List<String> oldLinks = Arrays.asList("/about", "/kernels");
        model.addAttribute("oldLinks", oldLinks);

        return "oldlinks";
    }
}