package vergilius;

import java.io.IOException;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vergilius.repos.FieldRepository;
import vergilius.repos.OsRepository;
import vergilius.repos.StructureRepository;


@Controller
public class MainController{
    @Autowired
    public OsRepository rep1;
    @Autowired
    public StructureRepository rep2;
    @Autowired
    public FieldRepository rep3;

    private List<Os> listOs;
    private List<Structure> listStr;
    private List<Field> listField;

    @GetMapping("/db")
    public String ShowRows(Model model) throws IOException {

/*
        Os os1 = new Os();
        os1.setIdopersys(1);
        os1.setOsname("Windows XP");
        rep1.save(os1);

        Structure str1 = new Structure();
        str1.setIdstruct(11);
        str1.setStructname("Unknown str1");
        str1.setStrsize(13);
        str1.setStrkind(Structure.Kind.UNION);
        str1.setOpersys(os1);
        rep2.save(str1);

        Field f1 = new Field();
        f1.setIdfield(1);
        f1.setFname("field_1");
        f1.setOffset(2);
        f1.setIdtype(14);
        f1.setStructure(str1);
        f1.setIsconst(true);
        f1.setIspointer(true);
        rep3.save(f1);
*/
        listOs = new ArrayList<>();
        for(Os i : rep1.findAll())
        {
            listOs.add(i);
        }
        model.addAttribute("osrows", listOs);


        listStr = new ArrayList<>();
        for(Structure j : rep2.findAll())
        {
            listStr.add(j);
        }
        model.addAttribute("strrows", listStr);

        listField = new ArrayList<>();
        for(Field k : rep3.findAll())
        {
            listField.add(k);
        }
        model.addAttribute("frows", listField);

        return "dbcontent";
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException {
        return "uploadForm";
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/";
    }

}
