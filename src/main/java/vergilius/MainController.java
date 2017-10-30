package vergilius;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import vergilius.Greeting;

@Controller
public class MainController {

    @GetMapping("/")
    public String greetingForm(Model model) {
        model.addAttribute("greeting", new Greeting());
        return "jgreeting";
    }
    @PostMapping("/")
    public String greetingSubmit(@ModelAttribute Greeting greeting)
    {
        return "jresult";
    }
}
