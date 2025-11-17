package au.edu.rmit.sept.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.model.Category;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

/**
 * Handles browsing and viewing event categories.
 * Displays category cards with event counts and basic statistics.
 * 
 * Follows same design pattern as EventController:
 *  - Constructor-based dependency injection
 *  - @GetMapping for UI pages
 *  - Model usage for Thymeleaf integration
 */
@Controller
public class BrowseCategoriesController {


    /**
     * Displays the "Browse Categories" page.
     * 
     * Fetches all available categories with their event counts and sends them to the view.
     * The HTML file: eventhub-browse-categories.html (usually in templates/events/)
     */
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final RSVPRepository rsvpRepository;

    public BrowseCategoriesController(EventRepository eventRepository,
                                      CategoryRepository categoryRepository,
                                      RSVPRepository rsvpRepository) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.rsvpRepository = rsvpRepository;
    }

    @GetMapping("/categories")
    public String showCategoriesPage(Model model) {
        model.addAttribute("pageTitle", "Browse Categories");

        // Stats
        long totalEvents = eventRepository.countActiveFutureEvents() + eventRepository.countActivePastEvents();
        long totalCategories = categoryRepository.count();
        long totalAttendees = rsvpRepository.countActiveRsvps();

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        long eventsThisWeek = eventRepository.countByEventDateBetween(weekStart, weekEnd);

        model.addAttribute("totalEvents", totalEvents);
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("totalAttendees", totalAttendees);
        model.addAttribute("eventsThisWeek", eventsThisWeek);

        // Dynamic categories with upcoming event counts
        List<Category> categories = categoryRepository.findAll();
        LocalTime nowTime = LocalTime.now();
        Map<Long, Long> categoryUpcomingCounts = categories.stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        c -> eventRepository.countUpcomingEventsByCategory(c.getId(), today, nowTime)
                ));

        model.addAttribute("categories", categories);
        model.addAttribute("categoryUpcomingCounts", categoryUpcomingCounts);

        // Per-category image mapping (name -> URL). Adjust as needed.
        Map<String, String> categoryImageUrls = new HashMap<>();
        // Use specified images from the provided sample (use image only, not code)
        categoryImageUrls.put("Technology", "https://www.brookings.edu/wp-content/uploads/2017/11/metro_20171121_tech-empowers-tech-polarizes-mark-muro.jpg?quality=75");
        categoryImageUrls.put("Music", "https://live-production.wcms.abc-cdn.net.au/a362273509f7eccdcf362bb73b3b006d?impolicy=wcms_crop_resize&cropH=788&cropW=1400&xPos=0&yPos=0&width=862&height=485");
        // Map Business to the 'Professional' image from the sample
        categoryImageUrls.put("Business", "https://orlandosydney.com/wp-content/uploads/2021/07/Business-Networking-Events-for-Professionals.-Photos-by-orlandosydney.com-OS1_5922.jpg");
        categoryImageUrls.put("Arts", "https://t3.ftcdn.net/jpg/02/73/22/74/360_F_273227473_N0WRQuX3uZCJJxlHKYZF44uaJAkh2xLG.jpg");
        // Explicit Social image (stable Unsplash static resource)
        categoryImageUrls.put("Social", "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?q=80&w=1600&auto=format&fit=crop");

        // Keep existing or fallback images for the rest
        categoryImageUrls.putIfAbsent("Sports", "https://images.unsplash.com/photo-1517649763962-0c623066013b?q=80&w=1600&auto=format&fit=crop");
        categoryImageUrls.putIfAbsent("Education", "https://images.unsplash.com/photo-1513258496099-48168024aec0?q=80&w=1600&auto=format&fit=crop");
        categoryImageUrls.putIfAbsent("Cultural", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?q=80&w=1600&auto=format&fit=crop");
        categoryImageUrls.putIfAbsent("Academic", "https://images.unsplash.com/photo-1558021212-51b6ecfa0db9?q=80&w=1600&auto=format&fit=crop");

        model.addAttribute("categoryImageUrls", categoryImageUrls);

        return "eventhub-browse-categories";
    }
}
