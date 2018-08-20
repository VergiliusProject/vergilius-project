package vergilius;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.yaml.snakeyaml.Yaml;

import org.yaml.snakeyaml.introspector.BeanAccess;
import vergilius.repos.TdataRepository;
import vergilius.repos.OsRepository;
import vergilius.repos.TtypeRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


@Controller
public class MainController{
    @Autowired
    public OsRepository rep1;
    @Autowired
    public TtypeRepository rep2;
    @Autowired
    public TdataRepository rep3;

    @GetMapping("/login")
    public String displayLogin(Model model) throws IOException {

        model.addAttribute("fam86", rep1.findByArch("x86"));
        model.addAttribute("fam64", rep1.findByArch("x64"));

        return "login";
    }
    @PostMapping("/login")
    public String handleLogin(@RequestParam(name="username") String username, @RequestParam(name="password") String password, HttpSession session, Model model) throws IOException {
        model.addAttribute(username);
        model.addAttribute(password);

        model.addAttribute("fam86", rep1.findByArch("x86"));
        model.addAttribute("fam64", rep1.findByArch("x64"));

        return "login";
    }

    @GetMapping("/admin")
    public String displayAdmin(Model model) throws IOException {

        model.addAttribute("fam86", rep1.findByArch("x86"));
        model.addAttribute("fam64", rep1.findByArch("x64"));

        return "admin";
    }

    @PostMapping("/admin")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {

        try(InputStream res = file.getInputStream()) {

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

            for(Ttype type: types)
            {
                type.setOpersys(os);
                Set<Tdata> datas = type.getData();

                if(datas != null)
                {
                    for(Tdata data: datas)
                    {
                        data.setTtype(type);
                    }
                }
            }
            os.setTypes(new HashSet<>(types));
            rep1.save(os);

        }
        catch(IOException e){}

        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/admin";
    }

  @GetMapping("/")
    public String displayHome(Model model)
    {
        //footer-links
        model.addAttribute("fam86", rep1.findByArch("x86"));
        model.addAttribute("fam64", rep1.findByArch("x64"));

        return "home";
    }

    @RequestMapping(value="/logout", method=RequestMethod.GET)
    public String logoutPage(Model model, HttpServletRequest request, HttpServletResponse response) {

        model.addAttribute("fam86", rep1.findByArch("x86"));
        model.addAttribute("fam64", rep1.findByArch("x64"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null){
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "/";
    }
    @GetMapping("/about")
    public String displayAbout(Model model)
    {
        model.addAttribute("fam86", rep1.findByArch("x86"));
        model.addAttribute("fam64", rep1.findByArch("x64"));

        return "about";
    }

    @GetMapping("/kernels")
    public String displayKernels(Model model)
    {
        //footer-links
        model.addAttribute("fam86", rep1.findByArch("x86"));
        model.addAttribute("fam64", rep1.findByArch("x64"));

        return "kernels";
    }

    @GetMapping("/kernels/x86")
    public String displayKernelsX86(Model model)
    {
        //footer-links and list of families
        model.addAttribute("fam86", rep1.findByArch("x86"));
        model.addAttribute("fam64", rep1.findByArch("x64"));

        return "x86";
    }
    @GetMapping("/kernels/x64")
    public String displayKernelsX64(Model model)
    {
        //footer-links and list of families
        model.addAttribute("fam86", rep1.findByArch("x86"));
        model.addAttribute("fam64", rep1.findByArch("x64"));

        return "x64";
    }

    @RequestMapping(value="/kernels/{arch:.+}/{famname:.+}")
    public String displayFamily(@PathVariable String arch, @PathVariable String famname, Model model)
    {
        List<Os> fam = rep1.findByArchAndFamily(arch, famname);
        model.addAttribute("fam", fam);

        model.addAttribute("fam86", rep1.findByArch("x86"));
        model.addAttribute("fam64", rep1.findByArch("x64"));

        return "family";
    }

    @RequestMapping(value = "/kernels/{arch:.+}/{famname:.+}/{osname:.+}", method = RequestMethod.GET)
    public String displayKinds(@PathVariable String arch, @PathVariable String famname, @PathVariable String osname, Model model)
    {
        Os opersys = rep1.findByArchAndFamilyAndOsname(arch, famname, osname);
        List<Ttype> reslist = rep2.findByOpersysAndIsConstFalseAndIsVolatileFalse(opersys);

        model.addAttribute("structs", Sorter.sortByName(Ttype.FilterByTypes(reslist, Ttype.Kind.STRUCT)));
        model.addAttribute("unions", Sorter.sortByName(Ttype.FilterByTypes(reslist, Ttype.Kind.UNION)));
        model.addAttribute("enums", Sorter.sortByName(Ttype.FilterByTypes(reslist, Ttype.Kind.ENUM)));

        model.addAttribute("fam86", rep1.findByArch("x86"));
        model.addAttribute("fam64", rep1.findByArch("x64"));

        return "ttype";
    }

    /* FieldBuilder!!! */
    @RequestMapping(value = "/kernels/{arch:.+}/{famname:.+}/{osname:.+}/{name:.+}", method = RequestMethod.GET)
    public String displayType(@PathVariable String arch, @PathVariable String famname,@PathVariable String osname,@PathVariable String name, Model model)
    {
        Os opersys = rep1.findByArchAndFamilyAndOsname(arch, famname, osname);

        String link = "/kernels/" + arch + "/" + famname + "/" + osname + "/";

        List<Ttype> typeslist = rep2.findByNameAndOpersys(name, opersys);

        if(typeslist != null && !typeslist.isEmpty()) {
            model.addAttribute("ttype", FieldBuilder.recursionProcessing(rep2, typeslist.get(0), 0, 0, link, opersys).toString());

            //search for cross-links
            List<Ttype> used_in = new ArrayList<>();

            for(Ttype i: typeslist)
            {
                used_in.addAll(rep2.findById1(i.getIdtype()));
                used_in.addAll(rep2.findById2(i.getIdtype()));
                used_in.addAll(rep2.findById3(i.getIdtype()));
                used_in.addAll(rep2.findById4(i.getIdtype()));
            }

            List<String> used_in_names = new ArrayList<>();
            for (Ttype i : used_in)
            {
                used_in_names.add(i.getName());
            }

            if (!used_in_names.isEmpty())
            {
                used_in_names = used_in_names.stream().distinct().sorted().collect(Collectors.toList());
            }
            else
            {
                used_in_names = null;
            }
            model.addAttribute("cros", used_in_names);
        }

        model.addAttribute("currOs", opersys);

        List<Os> os = Sorter.sortByBuildnumber(getListOs());
        Map<Os, Integer> map = new LinkedHashMap<>();
        Map<Integer, Os> mapInverted = new LinkedHashMap<>();
        for(int i = 1; i <= os.size(); i++)
        {
            map.put(os.get(i - 1), i);
            mapInverted.put(i, os.get(i - 1));
        }

        model.addAttribute("mapos", map);
        model.addAttribute("invertMapos", mapInverted);

        model.addAttribute("fam86", rep1.findByArch("x86"));
        model.addAttribute("fam64", rep1.findByArch("x64"));
        return "tdata";
    }


    public List<Os> getListOs()
    {
        List<Os> listOfOperSystems = new ArrayList<>();

        for(Os i : rep1.findAll())
        {
            listOfOperSystems.add(i);
        }
        return listOfOperSystems;
    }


}

