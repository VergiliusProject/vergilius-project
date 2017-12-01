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

        /*
        try(InputStream res = file.getInputStream()) {
            Yaml yaml = new Yaml();
            yaml.setBeanAccess(BeanAccess.FIELD);
            RootOs fromYaml = yaml.loadAs(res, RootOs.class);
            List<Os> mylist = fromYaml.getOpersystems();
            rep1.save(mylist);
            for(int i = 0; i < mylist.size(); i++) {
                System.out.println(mylist.get(i).getIdopersys() + " " + mylist.get(i).getOsname());
            }
        */

        /*
            Yaml yaml = new Yaml();
            yaml.setBeanAccess(BeanAccess.FIELD);
            Root fromYaml = yaml.loadAs(res, Root.class);

            List<Ttype> obj = fromYaml.getTypes();

            for(int i = 0; i < fromYaml.getTypes().size(); i++)
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

        }
        catch(IOException e){}
         */
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
        List<Ttype> reslist = Ttype.filterResults(rep2.findByOpersys(rep1.findByOsname(osname)));
        model.addAttribute("res", reslist);
        model.addAttribute("Struct", Ttype.Kind.STRUCT);
        model.addAttribute("Enum", Ttype.Kind.ENUM);
        model.addAttribute("Union", Ttype.Kind.UNION);
        return "ttype";
    }

    @RequestMapping(value = "/os/{osname:.+}/type/{name}", method = RequestMethod.GET)
    public String displayType(@PathVariable String osname,@PathVariable String name, Model model)
    {
        List<Tdata> datalist = new ArrayList<>();
        List<Ttype> typeslist = Ttype.filterResults(rep2.findByNameAndOpersys(name, rep1.findByOsname(osname)));

        for (Ttype t : typeslist)
        {
            datalist.addAll(rep3.findByTtype(t));
        }

        model.addAttribute("res1", datalist);
        return "tdata";
    }

}

