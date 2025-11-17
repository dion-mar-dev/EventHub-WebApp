package au.edu.rmit.sept.webapp.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import au.edu.rmit.sept.webapp.dto.AttendeeDTO;
import au.edu.rmit.sept.webapp.dto.BlockedAttendeeDTO;
import au.edu.rmit.sept.webapp.dto.CancelledRSVPDTO;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.UserService;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventApiController {

    private final EventService eventService;
    private final UserService userService;

    public EventApiController(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    /**
     * Fetches paginated attendees for an event - ORGANISER ONLY.
     */
    @GetMapping("/{eventId}/attendees")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getEventAttendees(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            Authentication authentication) {

        try {
            Long userId = userService.getUserIdByUsername(authentication.getName());
            Page<AttendeeDTO> attendees = eventService.getEventAttendeesAsOrganiser(
                    eventId, userId, search, PageRequest.of(page, size));

            Map<String, Object> response = new HashMap<>();
            response.put("attendees", attendees.getContent());
            response.put("currentPage", attendees.getNumber());
            response.put("totalPages", attendees.getTotalPages());
            response.put("totalElements", attendees.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cancels/blocks an attendee's RSVP - ORGANISER ONLY.
     */
    @PostMapping("/{eventId}/attendees/{attendeeId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelAttendeeRsvp(
            @PathVariable Long eventId,
            @PathVariable Long attendeeId,
            Authentication authentication) {

        try {
            Long userId = userService.getUserIdByUsername(authentication.getName());
            eventService.cancelAttendeeRsvpAsOrganiser(eventId, attendeeId, userId);

            return ResponseEntity.ok(Map.of("success", true,
                    "message", "RSVP cancelled successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
 * Exports event attendees as CSV file - ORGANISER ONLY.
 * Generates a downloadable CSV file containing all attendee information.
 * 
 * @param eventId The event ID
 * @param authentication The authenticated user
 * @return CSV file download
 */
@GetMapping("/{eventId}/attendees/export")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<byte[]> exportAttendeesToCSV(
        @PathVariable Long eventId,
        Authentication authentication) {
    
    try {
        Long userId = userService.getUserIdByUsername(authentication.getName());
        
        // Get CSV data from service
        byte[] csvData = eventService.exportAttendeesToCSVAsOrganiser(eventId, userId);
        
        // Get sanitized event title for filename
        String eventTitle = eventService.getEventTitle(eventId);
        String sanitizedTitle = eventService.sanitizeFilename(eventTitle);
        
        // Generate timestamp for filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = String.format("%s-attendees-%s.csv", sanitizedTitle, timestamp);
        
        // Set response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("no-cache");
        headers.setContentLength(csvData.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
                
    } catch (AccessDeniedException e) {
        return ResponseEntity.status(403).body(null);
    } catch (EntityNotFoundException e) {
        return ResponseEntity.status(404).body(null);
    } catch (IOException e) {
        return ResponseEntity.status(500).body(null);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(null);
    }
}

    /**
     * Blocks an attendee's RSVP - ORGANIZER/ADMIN ONLY.
     */
    @PostMapping("/{eventId}/attendees/{attendeeId}/block")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> blockAttendee(
            @PathVariable Long eventId,
            @PathVariable Long attendeeId,
            Authentication authentication) {

        try {
            Long userId = userService.getUserIdByUsername(authentication.getName());
            eventService.blockAttendeeAsOrganiser(eventId, attendeeId, userId);

            return ResponseEntity.ok(Map.of("success", true,
                    "message", "User blocked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Fetches paginated blocked users for an event - ORGANIZER/ADMIN ONLY.
     */
    @GetMapping("/{eventId}/blocked")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getBlockedUsers(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        try {
            Long userId = userService.getUserIdByUsername(authentication.getName());
            Page<BlockedAttendeeDTO> blockedUsers = eventService.getBlockedUsersAsOrganiser(
                    eventId, userId, PageRequest.of(page, size));

            Map<String, Object> response = new HashMap<>();
            response.put("blockedUsers", blockedUsers.getContent());
            response.put("currentPage", blockedUsers.getNumber());
            response.put("totalPages", blockedUsers.getTotalPages());
            response.put("totalElements", blockedUsers.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Unblocks a user - ORGANIZER/ADMIN ONLY.
     */
    @PostMapping("/{eventId}/blocked/{userId}/unblock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unblockUser(
            @PathVariable Long eventId,
            @PathVariable Long userId,
            Authentication authentication) {

        try {
            Long organizerId = userService.getUserIdByUsername(authentication.getName());
            eventService.unblockUserAsOrganiser(eventId, userId, organizerId);

            return ResponseEntity.ok(Map.of("success", true,
                    "message", "User unblocked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Fetches paginated cancelled RSVPs for an event - ORGANISER ONLY.
     */
    @GetMapping("/{eventId}/cancelled-rsvps")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCancelledRSVPs(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        try {
            Long userId = userService.getUserIdByUsername(authentication.getName());
            Page<CancelledRSVPDTO> cancelledRsvps = eventService.getCancelledRSVPs(
                    eventId, userId, PageRequest.of(page, size));

            Map<String, Object> response = new HashMap<>();
            response.put("cancelledRsvps", cancelledRsvps.getContent());
            response.put("currentPage", cancelledRsvps.getNumber());
            response.put("totalPages", cancelledRsvps.getTotalPages());
            response.put("totalElements", cancelledRsvps.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Process refund for a cancelled RSVP - ORGANISER ONLY.
     */
    @PostMapping("/{eventId}/cancelled-rsvps/{cancelledRsvpId}/refund")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> refundCancelledRSVP(
            @PathVariable Long eventId,
            @PathVariable Long cancelledRsvpId,
            Authentication authentication) {

        try {
            Long userId = userService.getUserIdByUsername(authentication.getName());
            eventService.refundCancelledRSVP(cancelledRsvpId, userId);

            return ResponseEntity.ok(Map.of("success", true,
                    "message", "Refund processed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}