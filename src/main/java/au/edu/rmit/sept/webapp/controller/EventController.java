package au.edu.rmit.sept.webapp.controller;

import java.security.Principal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import au.edu.rmit.sept.webapp.dto.DisplayReviewDTO;
import au.edu.rmit.sept.webapp.dto.EventCreateDTO;
// import au.edu.rmit.sept.webapp.dto.EventCreateDTO;
import au.edu.rmit.sept.webapp.dto.EventDetailsDTO;
import au.edu.rmit.sept.webapp.dto.ReviewDTO;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.KeywordService;
import au.edu.rmit.sept.webapp.service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@Controller
public class EventController {

    private final EventService eventService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final KeywordService keywordService;
    private final EventRepository eventRepository;
    private final ReviewService reviewService;
    private final RSVPRepository rsvpRepository;

    /**
     * Constructor injection - modern Spring best practice over @Autowired field injection.
     * Benefits: final fields ensure immutability and thread safety, fail-fast startup behavior,
     * easier unit testing with mock objects, no reflection overhead, explicit dependencies,
     * prevents circular dependencies. No @Autowired needed since Spring 4.3+.
     */
    public EventController(EventService eventService, UserRepository userRepository, 
                          CategoryRepository categoryRepository, KeywordService keywordService,
                          EventRepository eventRepository, ReviewService reviewService, RSVPRepository rsvpRepository) {
        this.eventService = eventService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.keywordService = keywordService;
        this.eventRepository = eventRepository;
        this.reviewService = reviewService;
        this.rsvpRepository = rsvpRepository;
    }

    /**
     * Checks if the current user has admin role.
     * Used for bypassing deactivated event restrictions.
     */
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Shows the details page for a specific event.
     * Fetches event details including category, RSVP status, and attendee counts.
     * Supports both authenticated and anonymous users.
     */
    @GetMapping("/events/{id}")
    public String showEventDetails(@PathVariable Long id,
                                   @RequestParam(required = false) String payment,
                                   Model model,
                                   Principal principal) {
        try {
            // Check if event is deactivated and user is not admin
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Event not found"));
            
            if (event.isDeactivated() && !isCurrentUserAdmin()) {
                throw new EntityNotFoundException("Event not found");
            }

            // Get current user ID if authenticated
            Long userId = null;
            if (principal != null) {
                User currentUser = userRepository.findByUsername(principal.getName())
                        .orElse(null);
                if (currentUser != null) {
                    userId = currentUser.getId();
                }
            }

            // Fetch event details through service
            EventDetailsDTO eventDetails = eventService.getEventById(id, userId);

            // Handle payment redirect messages
            if (payment != null && principal != null) {
                String username = principal.getName();

                // Find user's RSVP
                Optional<RSVP> userRsvp = rsvpRepository.findByUser_UsernameAndEvent_Id(username, id);

                if (userRsvp.isPresent()) {
                    String paymentStatus = userRsvp.get().getPaymentStatus();

                    if ("success".equals(payment)) {
                        if ("paid".equals(paymentStatus)) {
                            // Payment processed successfully
                            model.addAttribute("successMessage", "Payment successful! Your RSVP is confirmed.");
                        } else if ("pending".equals(paymentStatus)) {
                            // Webhook not processed yet
                            model.addAttribute("processingMessage", "Payment processing... Page will refresh automatically.");
                            model.addAttribute("autoRefresh", true);
                        }
                    } else if ("cancelled".equals(payment)) {
                        model.addAttribute("warningMessage", "Payment cancelled. Click 'Pay Now' to complete your RSVP.");
                    }
                }
            }

            //Check if the Event has passed
            LocalDateTime eventStartDateTime = LocalDateTime.of(eventDetails.getEventDate(), eventDetails.getEventTime());
            boolean eventHasPassed = eventStartDateTime.isBefore(LocalDateTime.now());

            //Check if the user was RSVPd when the event passed.
            boolean didUserRsvp = false;
            if (principal != null) {
                didUserRsvp = rsvpRepository.existsByUser_UsernameAndEvent_Id(principal.getName(), id);
            }

            // Fetch recent reviews, average rating, and total review count
            List<DisplayReviewDTO> reviews = reviewService.getRecentReviewsForEvent(id);
            double averageRating = reviewService.getAverageRatingForEvent(id);
            long totalReviews = reviewService.countReviewsForEvent(id);

            // Add event details to model
            model.addAttribute("event", eventDetails);

            // Add page title for layout
            model.addAttribute("pageTitle", eventDetails.getTitle());

            // Add Review data to the model
            model.addAttribute("reviews", reviews);
            model.addAttribute("averageRating", averageRating);
            model.addAttribute("totalReviews", totalReviews);
            model.addAttribute("didUserRsvp", didUserRsvp);
            model.addAttribute("eventHasPassed", eventHasPassed);

            // Empty Review DTO for keyword form
            model.addAttribute("reviewDTO", new ReviewDTO());

            // Generate QR code for event URL (general)
            String eventUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/events/")
                    .path(String.valueOf(id))
                    .queryParam("src", "qr")
                    .toUriString();
            String eventQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data="
                    + URLEncoder.encode(eventUrl, StandardCharsets.UTF_8);
            model.addAttribute("eventQrCodeUrl", eventQrUrl);

            // User ticket QR (per-RSVP) if RSVP exists
            if (eventDetails.getUserRsvpId() != null) {
                String ticketUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/events/")
                        .path(String.valueOf(id))
                        .queryParam("src", "ticket")
                        .queryParam("rid", eventDetails.getUserRsvpId())
                        .toUriString();
                String ticketQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data="
                        + URLEncoder.encode(ticketUrl, StandardCharsets.UTF_8);
                model.addAttribute("ticketQrCodeUrl", ticketQrUrl);
            }

            // Return the event details view
            return "events/event-details";

        } catch (EntityNotFoundException e) {
            // Handle event not found - redirect to home with error message
            model.addAttribute("errorMessage", "Event not found");
            return "redirect:/home";
        } catch (Exception e) {
            // Handle unexpected errors
            model.addAttribute("errorMessage", "An error occurred while loading the event");
            return "redirect:/home";
        }
    }

    // Defunct - can remove
    /** Show create event form */
    @GetMapping("/create")
    public String createEventForm(Model model) {
        model.addAttribute("event", new Event());
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("pageTitle", "Create Event");
        return "events/create";
    }

    /**
     * Shows the event creation form.
     * Fetches all categories for the dropdown and adds them to the model.
     * Requires authentication - Spring Security handles redirect if not logged in.
     */
    @GetMapping("/events/create")
    public String showCreateForm(Model model, Principal principal) {
        // Check if user is authenticated (Spring Security handles this)
        if (principal == null) {
            return "redirect:/login";
        }
        
        // Add empty DTO for form binding
        model.addAttribute("eventCreateDTO", new EventCreateDTO());
        
        // Add categories for dropdown
        model.addAttribute("categories", categoryRepository.findAll());
        
        // Add keywords for selection
        model.addAttribute("keywords", keywordService.getAllKeywords());
        
        return "events/create-event";
    }

    // /** Handle create event submission */
    // @PostMapping("/create")
    // public String saveEvent(@ModelAttribute("event") Event event,
    //                         @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
    //                         Authentication auth) {

    //     // Attach selected categories (if any)
    //     if (categoryIds != null && !categoryIds.isEmpty()) {
    //         List<Category> cats = categoryRepository.findAllById(categoryIds);
    //         event.setCategories(new HashSet<>(cats));
    //     } else {
    //         event.setCategories(new HashSet<>()); // none selected
    //     }

    //     // (Optional) attach current user as creator here if needed

    //     eventService.save(event);
    //     return "redirect:/events";
    // }
    /**
     * Processes event creation form submission.
     * Validates the DTO, handles validation errors, and creates the event.
     */
    @PostMapping("/events/create")
    public String createEvent(@Valid @ModelAttribute EventCreateDTO eventCreateDTO,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        // Check authentication
        if (principal == null) {
            return "redirect:/login";
        }

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            // Re-add categories for form redisplay
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("keywords", keywordService.getAllKeywords());
            return "events/create-event";
        }

        try {
            // Get current user
            User currentUser = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            // Create event through service
            Long eventId = eventService.createEvent(eventCreateDTO, currentUser);

            // Add success message
            redirectAttributes.addFlashAttribute("successMessage",
                    "Event created successfully!");

            // Redirect to event details page
            return "redirect:/events/" + eventId;

        } catch (IllegalArgumentException e) {
            // Handle service-layer validation errors (e.g., date/time in past)
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("keywords", keywordService.getAllKeywords());
            return "events/create-event";
        } catch (Exception e) {
            // Handle unexpected errors
            model.addAttribute("errorMessage",
                    "An error occurred while creating the event. Please try again.");
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("keywords", keywordService.getAllKeywords());
            return "events/create-event";
        }
    }

    /**
     * Deletes an event. Only allows deletion by the event creator and only if the event hasn't started.
     * Uses POST method for security (prevents accidental deletion via GET requests).
     */
    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id, 
                              Principal principal, 
                              RedirectAttributes redirectAttributes) {
        
        // Check authentication
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            // Check if event is deactivated and user is not admin
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Event not found"));
            
            if (event.isDeactivated() && !isCurrentUserAdmin()) {
                throw new EntityNotFoundException("Event not found");
            }

            // Get current user
            User currentUser = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            // Delete event through service (handles creator validation and event started check)
            eventService.deleteEvent(id, currentUser.getId());

            // Add success message
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Event deleted successfully");

            // Redirect to home page
            return "redirect:/";

        } catch (EntityNotFoundException e) {
            // Event not found
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Event not found");
            return "redirect:/";
        } catch (AccessDeniedException e) {
            // User is not the creator or event has started
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "You can only delete events you created and that haven't started yet");
            return "redirect:/events/" + id;
        } catch (Exception e) {
            // Handle unexpected errors
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "An error occurred while deleting the event");
            return "redirect:/events/" + id;
        }
    }
}