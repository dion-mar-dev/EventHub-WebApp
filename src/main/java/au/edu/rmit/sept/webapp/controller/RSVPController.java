package au.edu.rmit.sept.webapp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.service.RSVPService;
import au.edu.rmit.sept.webapp.service.StripeService;
import au.edu.rmit.sept.webapp.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/rsvp")
public class RSVPController {

    private final RSVPService rsvpService;
    private final EventRepository eventRepository;
    private final UserService userService;
    private final StripeService stripeService;
    private final RSVPRepository rsvpRepository;

    public RSVPController(RSVPService rsvpService,
            EventRepository eventRepository,
            UserService userService,
            StripeService stripeService,
            RSVPRepository rsvpRepository) {
        this.rsvpService = rsvpService;
        this.eventRepository = eventRepository;
        this.userService = userService;
        this.stripeService = stripeService;
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
     * Handle RSVP creation.
     * For paid events, redirects to Stripe checkout.
     * For free events, creates RSVP and redirects back to event page.
     */
    @PostMapping("/{eventId}")
    public String createRSVP(@PathVariable Long eventId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            // Get current user
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Get event
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found"));

            // Create RSVP
            RSVP rsvp = rsvpService.createRSVP(user, event);

            // Show success message
            if (event.getRequiresPayment() && event.getPrice() != null) {
                redirectAttributes.addFlashAttribute("success",
                        "RSVP created! Click 'Pay Now' to complete payment and confirm your attendance.");
            } else {
                redirectAttributes.addFlashAttribute("success",
                        "Successfully RSVP'd to " + event.getTitle());
            }
            return "redirect:/events/" + eventId;

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/events/" + eventId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "An error occurred while processing your RSVP");
            return "redirect:/events/" + eventId;
        }
    }

    @PostMapping("/cancel")
    public String cancelRSVP(@RequestParam Long eventId,
            Authentication auth,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            // Check if event is deactivated and user is not admin
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new EntityNotFoundException("Event not found"));

            if (event.isDeactivated() && !isCurrentUserAdmin()) {
                throw new EntityNotFoundException("Event not found");
            }

            // Call service to cancel RSVP
            rsvpService.cancelRSVP(eventId, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage", "RSVP cancelled successfully");
        } catch (RuntimeException e) {
            // Handle errors
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        // Redirect back to the previous page, or to event details if no referer
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        } else {
            return "redirect:/events/" + eventId;
        }
    }
}