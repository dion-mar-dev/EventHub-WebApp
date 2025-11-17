package au.edu.rmit.sept.webapp.controller;

import au.edu.rmit.sept.webapp.dto.UserRegistrationDTO;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
// The Spring Model object is somewhat of a misnomer. In true MVC architecture:
// - Model=your entity classes(User,Event,etc.) under the directory "Model" - the data/business layer
// - View=Thymeleaf templates
// - Controller=your controller classes
// The Spring Model object is just a data transfer utility-it'spart of the View mechanism,not the architectural Model layer.
// Spring Model object-it'sa key-value map that carries data from the controller to the Thymeleaf template.
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
public class AuthController {
    
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    
    /**
     * Constructor injection - modern Spring best practice over @Autowired field injection.
     * Benefits: final fields ensure immutability and thread safety, fail-fast startup behavior,
     * easier unit testing with mock objects, no reflection overhead, explicit dependencies,
     * prevents circular dependencies. No @Autowired needed since Spring 4.3+.
     */
    public AuthController(UserService userService, CategoryRepository categoryRepository) {
        this.userService = userService;
        this.categoryRepository = categoryRepository;
    }
    
    // handles user registration, registration form request
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        // Redirect authenticated users to home
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/home";
        }
        
        // add empty DTO for form binding
        model.addAttribute("userRegistrationDTO", new UserRegistrationDTO());
        
        // add categories for checkboxes
        model.addAttribute("categories", categoryRepository.findAll());
        
        return "auth/register";
    }
    
    // process user registration form submission
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("userRegistrationDTO") UserRegistrationDTO registrationDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        
        // check for validation errors, if errors exist, re-display form
        if (bindingResult.hasErrors()) {
            // re-add categories to model for form redisplay
            model.addAttribute("categories", categoryRepository.findAll());
            return "auth/register";  // EDITED: Corrected the return view name
        }
        
        try {
            // attempt to register user
            userService.register(registrationDTO);
            
            // registration successful, redirect to login with success message
            redirectAttributes.addAttribute("success", "true");
            return "redirect:/login";
            
        } catch (IllegalArgumentException e) {
            // handle business logic validation errors, e.g. duplicate username, email, or password mismatch
            String errorMessage = e.getMessage();
            
            if (errorMessage.contains("Username already taken")) {
                bindingResult.rejectValue("username", "error.username", "Username already taken");
            } else if (errorMessage.contains("Email already registered")) {
                bindingResult.rejectValue("email", "error.email", "Email already registered");
            } else if (errorMessage.contains("Passwords do not match")) {
                bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
            } else {
                bindingResult.reject("error.global", errorMessage);
            }
            
            // re-add categories to model for form redisplay
            model.addAttribute("categories", categoryRepository.findAll());
            return "auth/register";
        }
    }
    
    /**
     * displays login form, handles login error and logout success messages
     * redirects authenticated users to home page, spring security automatically handles post /login authentication.
     * Empty field validation is handled client-side with login.html having
     * required attributes on username and password fields.
     */
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "logout", required = false) String logout,
                               @RequestParam(value = "success", required = false) String success,
                               @RequestParam(value = "expired", required = false) String expired,
                               Model model) {
        
        // Redirect authenticated users to home
        // Authentication object represents the current user's security context
        // SecurityContextHolder.getContext().getAuthentication() retrieves this from
        // Spring Security's thread-local storage where it's maintained throughout the
        // user's session.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/home";  //EDITED AW
        }
        // add login error message if authentication failed
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password");
        }

        // add logout success message if user logged out
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been successfully logged out");
        }
        
        // add registration success message if redirected from registration
        if (success != null) {
            model.addAttribute("successMessage", "Registration successful! Please log in");
        }
        
        // add session expired message if session timed out
        if (expired != null) {
            model.addAttribute("expiredMessage", "Session expired. Please login again");
        }
        
        // remember me functionality could be added here in future implementation
        return "auth/login";
    }
}