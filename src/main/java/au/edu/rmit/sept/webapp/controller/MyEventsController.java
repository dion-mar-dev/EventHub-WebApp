package au.edu.rmit.sept.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

import au.edu.rmit.sept.webapp.dto.EventCardDTO;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.UserService;

import java.util.List;
import java.util.ArrayList;

/**
 * MyEventsController handles user's personal event management pages.
 * 
 * This controller serves the "/my-events" endpoint for authenticated users only.
 * It displays events the user has RSVP'd to and events they have created.
 * Anonymous users will be redirected to login by Spring Security configuration.
 */
@Controller
public class MyEventsController {
    
    private final EventService eventService;
    private final UserService userService;
    
    /**
     * Constructor injection following the same pattern as HomeController.
     * Benefits include immutability, fail-fast startup, and easier testing.
     */
    public MyEventsController(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }
    
    /**
     * My Events page endpoint that serves both RSVP'd and created events for authenticated users.
     * 
     * This method displays both tabs:
     * - Events that the authenticated user has RSVP'd to but did not create
     * - Events that the authenticated user has created
     * It follows the same authentication pattern as HomeController but requires the user to be logged in.
     * 
     * @param authentication Spring Security's representation of the current user
     * @param model Spring MVC's data carrier for transporting data to the template
     * @return "my-events" template name, which resolves to templates/my-events.html
     */
    @GetMapping("/my-events")
    public String showMyEvents(Authentication authentication, Model model) {
        Long userId = null;
        
        // Get authenticated user ID
        // At this endpoint, authentication should never be null or anonymous
        // since Spring Security will redirect unauthenticated users to login
        if (authentication != null && !authentication.getName().equals("anonymousUser")) {
            userId = userService.getUserIdByUsername(authentication.getName());
            model.addAttribute("username", authentication.getName());
        }
        
        // Fetch events the user has RSVP'd to
        List<EventCardDTO> rsvpEvents;
        try {
            rsvpEvents = eventService.getUserRSVPEvents(userId);
        } catch (Exception e) {
            e.printStackTrace();
            rsvpEvents = new ArrayList<>();
        }
        
        if (rsvpEvents == null) {
            rsvpEvents = new ArrayList<>();
        }
        
        model.addAttribute("rsvpEvents", rsvpEvents);
        
        // Fetch events the user has created
        List<EventCardDTO> createdEvents;
        try {
            createdEvents = eventService.getUserCreatedEvents(userId);
        } catch (Exception e) {
            e.printStackTrace();
            createdEvents = new ArrayList<>();
        }
        
        if (createdEvents == null) {
            createdEvents = new ArrayList<>();
        }
        
        model.addAttribute("createdEvents", createdEvents);
        
        // Return template name - resolves to src/main/resources/templates/my-events.html
        return "my-events";
    }
}