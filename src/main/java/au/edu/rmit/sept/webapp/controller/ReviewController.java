package au.edu.rmit.sept.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import au.edu.rmit.sept.webapp.dto.EventCreateDTO;
// import au.edu.rmit.sept.webapp.dto.EventCreateDTO;
import au.edu.rmit.sept.webapp.dto.EventDetailsDTO;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;

import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;

import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.KeywordService;
import au.edu.rmit.sept.webapp.service.ReviewService;

import au.edu.rmit.sept.webapp.dto.KeywordDTO;
import au.edu.rmit.sept.webapp.dto.ReviewDTO;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.security.Principal;

@Controller
public class ReviewController {
    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    //Constructor
    public ReviewController(ReviewService reviewService, UserRepository userRepository, EventRepository eventRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    @PostMapping("/events/{eventId}/review")
    public String submitReview(@PathVariable Long eventId,
                            @ModelAttribute ReviewDTO reviewDTO,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {

        try {
            reviewService.saveReview(reviewDTO, eventId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Review submitted!");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "You have already submitted a review for this event.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/events/" + eventId;
    }
    
}
