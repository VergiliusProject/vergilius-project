package vergilius;

import java.io.*;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
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


@Controller
public class MainController{
    @Autowired
    public OsRepository rep1;
    @Autowired
    public TtypeRepository rep2;
    @Autowired
    public TdataRepository rep3;

    private List<Os> listOs;
    private List<Ttype> listTypes;
    private List<Tdata> listData;

    /*unused method*/
    @GetMapping("/db")
    public String showAllContent(Model model) throws IOException {

        listOs = new ArrayList<>();
        for(Os i : rep1.findAll())
        {
            listOs.add(i);
        }
        model.addAttribute("osrows", listOs);


        listTypes = new ArrayList<>();
        for(Ttype j : rep2.findAll())
        {
            listTypes.add(j);
        }
        model.addAttribute("trows", listTypes);

        listData = new ArrayList<>();
        for(Tdata k : rep3.findAll())
        {
            listData.add(k);
        }
        model.addAttribute("drows", listData);

        return "dbcontent";
    }

    @GetMapping("/admin")
    public String displayUploadForm(Model model) throws IOException {
        return "uploadForm";
    }

    @PostMapping("/admin")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {

        try(InputStream res = file.getInputStream()) {
/*
            Yaml yaml = new Yaml();
            yaml.setBeanAccess(BeanAccess.FIELD);
            RootOs fromYaml = yaml.loadAs(res, RootOs.class);
            List<Os> mylist = fromYaml.getOpersystems();
            rep1.save(mylist);
*/
/*
            Yaml yaml = new Yaml();
            yaml.setBeanAccess(BeanAccess.FIELD);
            Root fromYaml = yaml.loadAs(res, Root.class);

            List<Ttype> obj = fromYaml.getTypes();

            for(int i = 0; i < obj.size(); i++)
            {
                Set<Tdata> tmp = obj.get(i).getData();

                if(tmp != null)
                {
                    Iterator<Tdata> iter = tmp.iterator();
                    while (iter.hasNext()) {
                        Tdata record = iter.next();
                        record.setTtype(obj.get(i));
                    }
                }
            }
            rep2.save(obj);
*/
        }
        catch(IOException e){}

        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/admin";
    }

    @GetMapping("/")
    public String displayHome(Model model)
    {
       List<Os> listOfOperSystems = new ArrayList<>();
        for(Os i : rep1.findAll())
        {
            listOfOperSystems.add(i);
        }
        model.addAttribute("os", listOfOperSystems);
        return "home";
    }

    @GetMapping("/about")
    public String displayAbout(Model model)
    {
        return "about";
    }

    @RequestMapping(value = "/os/{osname:.+}", method = RequestMethod.GET)
    public String displayOs(@PathVariable String osname, Model model)
    {
        Os opersys = rep1.findByOsname(osname);
        List<Ttype> reslist = rep2.findByOpersysAndIsConstFalseAndIsVolatileFalse(opersys);

        model.addAttribute("structs", Ttype.FilterByTypes(reslist, Ttype.Kind.STRUCT));
        model.addAttribute("unions", Ttype.FilterByTypes(reslist, Ttype.Kind.UNION));
        model.addAttribute("enums", Ttype.FilterByTypes(reslist, Ttype.Kind.ENUM));

        return "ttype";
    }

    @RequestMapping(value = "/os/{osname:.+}/type/{name}", method = RequestMethod.GET)
    public String displayType(@PathVariable String osname,@PathVariable String name, Model model)
    {
        Os opersys = rep1.findByOsname(osname);
        List<Ttype> typeslist = rep2.findByNameAndOpersysAndIsConstFalseAndIsVolatileFalse(name, opersys);

        List<String> enumsArr = new ArrayList<>();

        for(Ttype t: Ttype.FilterByTypes(typeslist, Ttype.Kind.ENUM))
        {
            enumsArr.add(EnumConverter.converts(t));
        }

        List<String> structsArr = new ArrayList<>();

        for(Ttype t: Ttype.FilterByTypes(typeslist, Ttype.Kind.STRUCT))
        {
            structsArr.add(StructConverter.converts(t,rep2));
        }

        List<String> unionsArr = new ArrayList<>();

        for(Ttype t: Ttype.FilterByTypes(typeslist, Ttype.Kind.UNION))
        {
            structsArr.add(UnionConverter.converts(t, rep2));
        }

        model.addAttribute("res1", enumsArr);
        model.addAttribute("res2", structsArr);
        model.addAttribute("res3", unionsArr);

        return "tdata";
    }

}

