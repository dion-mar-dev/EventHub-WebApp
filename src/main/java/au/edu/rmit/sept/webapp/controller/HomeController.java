package au.edu.rmit.sept.webapp.controller;

import org.springframework.stereotype.Controller;
// The Spring Model object is somewhat of a misnomer. In true MVC architecture:
// - Model=your entity classes(User,Event,etc.) under the directory "Model" - the data/business layer
// - View=Thymeleaf templates
// - Controller=your controller classes
// The Spring Model object is just a data transfer utility-it'spart of the View mechanism,not the architectural Model layer.
// Spring Model object-it'sa key-value map that carries data from the controller to the Thymeleaf template.
//   i.e. Spring Model object = data carrier between controller and view
//   Spring automatically injects this Model parameter into controller methods. You
//   put data into it with addAttribute(), and Thymeleaf templates access that data
//   using ${key} syntax.
//   Spring's Model is about MVC data transfer, while my model entities are about database structure.
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import au.edu.rmit.sept.webapp.dto.EventCardDTO;
import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.KeywordService;
import au.edu.rmit.sept.webapp.service.UserService;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.service.KeywordService;
import au.edu.rmit.sept.webapp.model.Keyword;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.security.core.Authentication;

/**
 * HomeController handles the main landing page functionality.
 * 
 * This controller serves the "/" endpoint to both authenticated and anonymous users.
 * SecurityConfig.java permits all users to access "/" without requiring login.
 * The home page displays upcoming events with personalized RSVP status for logged-in users.
 */
@Controller
public class HomeController {
    
    private final EventService eventService;
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final KeywordService keywordService;
    
    /**
     * Constructor injection - modern Spring best practice over @Autowired field injection.
     * Benefits: 1) final fields ensure immutability and thread safety by preventing
     * accidental reassignment after construction, 2) fail-fast startup behavior catches
     * missing dependencies at application boot rather than runtime, 3) easier unit testing
     * since you can pass mock objects directly through constructor without needing Spring
     * context, 4) no reflection overhead for better performance and clearer code paths,
     * 5) explicit dependencies visible in constructor signature make class requirements
     * obvious to other developers, 6) prevents circular dependencies which would cause
     * impossible construction scenarios. No @Autowired annotation needed since Spring 4.3+
     * automatically detects single constructor for dependency injection.
     */
    public HomeController(EventService eventService, 
                         UserService userService, 
                         CategoryRepository categoryRepository,
                         KeywordService keywordService) {
        this.eventService = eventService;
        this.userService = userService;
        this.categoryRepository = categoryRepository;
        this.keywordService = keywordService;
    }

    /**
     * Main home page endpoint that serves events to all users.
     * 
     * This method implements the core MVP functionality where:
     * - Anonymous users see all upcoming events without RSVP status
     * - Authenticated users see the same events plus their personal RSVP status
     * 
     * The method handles both user types through conditional logic based on
     * authentication state.
     * Spring Security automatically injects the Authentication object representing
     * the current user context.
     * 
     * @param tab            Tab parameter to switch between "upcoming" and "past"
     *                       events
     * @param categoryId     Filter by specific category
     * @param fromDate       Filter events from this date onwards
     * @param keywordIds     Filter by keyword IDs
     * @param searchTerm     Search term for event filtering
     * @param page           Current page number for pagination
     * @param size           Number of items per page
     * @param authentication Spring Security's representation of the current user
     * @param model          Spring MVC's data carrier that transports data from
     *                       controller to Thymeleaf template
     * @return "home" template name, which resolves to templates/home.html
     */
    @GetMapping("/")
    public String showHomePage(@RequestParam(required = false) String tab,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) Set<Long> keywordIds,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            Authentication authentication, Model model) {

        Long userId = null;

        // Get authenticated user ID and username
        if (authentication != null && !"anonymousUser".equals(authentication.getName())) {
            userId = userService.getUserIdByUsername(authentication.getName());
            model.addAttribute("username", authentication.getName());
        }

        // Determine active tab
        String activeTab = (tab != null) ? tab : "upcoming";
        model.addAttribute("activeTab", activeTab);

        // Fetch events based on active tab with error handling
        Page<EventCardDTO> eventsPage;
        try {
            if ("past".equals(activeTab)) {
                eventsPage = eventService.getPastEvents(userId, PageRequest.of(page, size));
                model.addAttribute("isPastTab", true);
            } else {
                eventsPage = eventService.getUpcomingEvents(userId, categoryId, fromDate, keywordIds, searchTerm,
                        PageRequest.of(page, size));
                model.addAttribute("isPastTab", false);
            }
        } catch (Exception e) {
            // Log the error (consider adding proper logging)
            e.printStackTrace();
            eventsPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }

        // Null safety check
        if (eventsPage == null) {
            eventsPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }

        model.addAttribute("events", eventsPage);

        // Get categories for filter dropdown
        List<Category> categories = categoryRepository.findAll();
        if (categories == null) {
            categories = new ArrayList<>();
        }
        model.addAttribute("categories", categories);

        // Get keywords for filter
        List<au.edu.rmit.sept.webapp.dto.KeywordDTO> keywords = keywordService.getAllKeywords();
        model.addAttribute("keywords", keywords);

        // Add filter parameters to model for maintaining form state
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedKeywordIds", keywordIds);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("searchTerm", searchTerm);

        // Add selected category name for display in filter indicator
        if (categoryId != null) {
            categories.stream()
                    .filter(cat -> cat.getId().equals(categoryId))
                    .findFirst()
                    .ifPresent(cat -> model.addAttribute("selectedCategoryName", cat.getName()));
        }

        // Add selected Keyword objects for enhanced display
        if (keywordIds != null && !keywordIds.isEmpty()) {
            Set<Keyword> selectedKeywords = keywordService.findKeywordsByIds(keywordIds);
            model.addAttribute("selectedKeywords", selectedKeywords);
        }

        // Get recommended events for authenticated users
        List<EventCardDTO> recommendedEvents = eventService.getRecommendedEvents(userId);
        model.addAttribute("recommendedEvents", recommendedEvents);

        return "home";
    }

    /**
     * Legacy /home endpoint - redirects to the root endpoint.
     * Maintained for backward compatibility with existing bookmarks and links.
     * 
     * @return Redirect to the root "/" endpoint
     */
    @GetMapping("/home")
    public String homeRedirect() {
        return "redirect:/";
    }
    
    // ORPHANED CODE, WILL DELETE SOON
    /**
     * Main home page endpoint that serves events to all users.
     * 
     * This method implements the core MVP functionality where:
     * - Anonymous users see all upcoming events without RSVP status
     * - Authenticated users see the same events plus their personal RSVP status
     * 
     * The method handles both user types through conditional logic based on authentication state.
     * Spring Security automatically injects the Authentication object representing the current user context.
     * 
     * @param authentication Spring Security's representation of the current user (never null in practice)
     * @param model Spring MVC's data carrier that transports data from controller to Thymeleaf template
     * @return "home" template name, which resolves to templates/home.html
     */
    /*
    @GetMapping("/")
    public String showHomePage(@RequestParam(required = false) String tab,  // Add tab parameter
                              @RequestParam(required = false) Long categoryId,
                              @RequestParam(required = false) LocalDate fromDate, 
                              @RequestParam(required = false) Set<Long> keywordIds,
                              @RequestParam(required = false) String searchTerm,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "24") int size,
                              Authentication authentication, Model model) {
        
        Long userId = null;
        
        // Existing authentication logic...
        if (authentication != null && !"anonymousUser".equals(authentication.getName())) {
            userId = userService.getUserIdByUsername(authentication.getName());
        }
        
        // Determine active tab
        String activeTab = (tab != null) ? tab : "upcoming";
        model.addAttribute("activeTab", activeTab);
        
        // Load data based on active tab
        if ("past".equals(activeTab)) {
            // Past events - simple pagination only
            Page<EventCardDTO> pastEvents = eventService.getPastEvents(userId, 
                    PageRequest.of(page, size));
            model.addAttribute("events", pastEvents);
            model.addAttribute("isPastTab", true);
            
            // Clear filter attributes for past tab
            model.addAttribute("selectedCategoryId", null);
            model.addAttribute("selectedCategoryName", null);
            model.addAttribute("fromDate", null);
            model.addAttribute("selectedKeywordIds", null);
            model.addAttribute("searchTerm", null);
            
        } else {
            // Upcoming events - existing logic with filters
            Pageable pageable = PageRequest.of(page, size);
            Page<EventCardDTO> upcomingEvents = eventService.getUpcomingEvents(
                    userId, categoryId, fromDate, keywordIds, searchTerm, pageable);
            model.addAttribute("events", upcomingEvents);
            model.addAttribute("isPastTab", false);
            
            // Existing filter attribute logic...
            model.addAttribute("selectedCategoryId", categoryId);
            if (categoryId != null) {
                categoryRepository.findById(categoryId).ifPresent(category ->
                        model.addAttribute("selectedCategoryName", category.getName())
                );
            }
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("selectedKeywordIds", keywordIds);
            model.addAttribute("searchTerm", searchTerm);
        }
        
        // Common data for both tabs
        List<EventCardDTO> recommendedEvents = eventService.getRecommendedEvents(userId);
        model.addAttribute("recommendedEvents", recommendedEvents);
        
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        
        List<au.edu.rmit.sept.webapp.dto.KeywordDTO> keywords = keywordService.getAllKeywords();
        model.addAttribute("keywords", keywords);
        
        return "home";
    }
    */
    
    // ORPHANED CODE, WILL DELETE SOON
    /*
    @GetMapping("/home")
    public String home(@RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) LocalDate fromDate,
                        @RequestParam(required = false) Set<Long> keywordIds,
                        @RequestParam(required = false) String searchTerm,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "24") int size, 
                        Authentication authentication, Model model) {
        Long userId = null;
        
        if (authentication != null && !authentication.getName().equals("anonymousUser")) {
            userId = userService.getUserIdByUsername(authentication.getName());
            model.addAttribute("username", authentication.getName());
        }
        
        Page<EventCardDTO> eventsPage;
        try {
            eventsPage = eventService.getUpcomingEvents(userId, categoryId, fromDate, keywordIds, searchTerm, PageRequest.of(page, size));
        } catch (Exception e) {
            eventsPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }
        
        if (eventsPage == null) {
            eventsPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }
        
        model.addAttribute("events", eventsPage);
        
        List<Category> categories = categoryRepository.findAll();
        if (categories == null) {
            categories = new ArrayList<>();
        }
        model.addAttribute("categories", categories);

        List<au.edu.rmit.sept.webapp.dto.KeywordDTO> keywords = keywordService.getAllKeywords();
        model.addAttribute("keywords", keywords);

        // Add selected categoryId to model for maintaining dropdown state
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedKeywordIds", keywordIds);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("searchTerm", searchTerm);

        // Add selected category name for display in filter indicator
        if (categoryId != null) {
            categories.stream()
                    .filter(cat -> cat.getId().equals(categoryId))
                    .findFirst()
                    .ifPresent(cat -> model.addAttribute("selectedCategoryName", cat.getName()));
        }
        
        if (keywordIds != null && !keywordIds.isEmpty()) {
            Set<Keyword> selectedKeywords = keywordService.findKeywordsByIds(keywordIds);
            model.addAttribute("selectedKeywords", selectedKeywords);
        }

        List<EventCardDTO> recommendedEvents = eventService.getRecommendedEvents(userId);
        model.addAttribute("recommendedEvents", recommendedEvents);
        
        
        return "home";
    }
    */
}
