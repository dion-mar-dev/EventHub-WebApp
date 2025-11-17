package au.edu.rmit.sept.webapp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;

import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.model.BlockedRSVP;
import au.edu.rmit.sept.webapp.model.CancelledRSVP;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.repository.BlockedRSVPRepository;
import au.edu.rmit.sept.webapp.repository.CancelledRSVPRepository;
import au.edu.rmit.sept.webapp.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class RSVPService {

    private final RSVPRepository rsvpRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BlockedRSVPRepository blockedRSVPRepository;
    private final CancelledRSVPRepository cancelledRSVPRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;

    /**
     * Checks if a user has the ADMIN role.
     * @param userId The ID of the user to check
     * @return true if the user has the ADMIN role, false otherwise
     */
    private boolean isUserAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return userService.hasRole(userId, "ROLE_ADMIN");
    }

    // Add this to RSVPService.java - replace existing createRSVP method

    /**
     * Creates a new RSVP for a user to an event.
     * For paid events, sets payment status to 'pending'.
     * For free events, payment status remains null.
     */
    @Transactional
    public RSVP createRSVP(User user, Event event) {
        // Check if user is blocked from this event
        if (blockedRSVPRepository.existsByEventAndUser(event, user)) {
            throw new IllegalStateException("You are blocked from RSVPing to this event");
        }

        // Check if RSVP already exists
        if (rsvpRepository.existsByUserAndEvent(user, event)) {
            throw new IllegalStateException("You have already RSVP'd to this event");
        }

        // Check if event is full
        if (event.getCapacity() != null) {
            long currentAttendees = rsvpRepository.countByEvent(event);
            if (currentAttendees >= event.getCapacity()) {
                throw new IllegalStateException("This event is full");
            }
        }

        // Create RSVP
        RSVP rsvp = new RSVP(user, event);

        // Set payment status for paid events
        if (event.getRequiresPayment() && event.getPrice() != null) {
            rsvp.setPaymentStatus("pending");
            // amountPaid remains null until payment completes
        }

        return rsvpRepository.save(rsvp);
    }

    public void cancelRSVP(Long eventId, String username) {
        // Find RSVP
        RSVP rsvp = rsvpRepository.findByUser_UsernameAndEvent_Id(username, eventId)
                .orElseThrow(() -> new RuntimeException("RSVP not found"));

        // If paid event, create cancelled RSVP record
        if (rsvp.getEvent().getRequiresPayment() && rsvp.getPaymentStatus() != null) {
            CancelledRSVP cancelledRsvp = new CancelledRSVP();
            cancelledRsvp.setRsvpId(rsvp.getId());
            cancelledRsvp.setUser(rsvp.getUser());
            cancelledRsvp.setEvent(rsvp.getEvent());
            cancelledRsvp.setInitiatedBy("attendee");
            cancelledRsvp.setCancelledBy(rsvp.getUser()); // User cancelled their own RSVP
            cancelledRsvp.setPaymentStatus(rsvp.getPaymentStatus());
            cancelledRsvp.setAmountPaid(rsvp.getAmountPaid());
            cancelledRsvp.setStripePaymentIntentId(rsvp.getStripePaymentIntentId());
            cancelledRSVPRepository.save(cancelledRsvp);
        }

        // Delete associated payment records first to avoid FK constraint violation
        paymentRepository.deleteByRsvp(rsvp);

        // Delete RSVP
        rsvpRepository.delete(rsvp);
    }

    // Helper method to get attendee count
    public Long getAttendeeCount(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElse(null);
        if (event == null)
            return 0L;
        return rsvpRepository.countByEvent(event);
    }

    /**
     * Blocks a user from RSVPing to an event - ORGANIZER/ADMIN ONLY.
     * Deletes their current RSVP and prevents future RSVPs.
     *
     * @param eventId     The event ID
     * @param userId      The user ID to block
     * @param organizerId The user ID of the requester (must be event organizer or admin)
     * @throws AccessDeniedException if user is not the event organizer or admin
     */
    @Transactional
    public void blockUserFromEventAsOrganiser(Long eventId, Long userId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Organizer not found"));

        // Verify the requester is the event organizer or an admin
        if (!event.getCreatedBy().getId().equals(organizerId) && !isUserAdmin(organizerId)) {
            throw new AccessDeniedException("Only the event organizer or admin can block users");
        }

        // Check if already blocked
        if (blockedRSVPRepository.existsByEventAndUser(event, user)) {
            throw new RuntimeException("User is already blocked from this event");
        }

        // If user has RSVP, create cancelled record before deleting
        Optional<RSVP> existingRsvp = rsvpRepository.findByUserAndEvent(user, event);
        if (existingRsvp.isPresent()) {
            RSVP rsvp = existingRsvp.get();

            // Create cancelled record if paid event
            if (event.getRequiresPayment()) {
                CancelledRSVP cancelledRsvp = new CancelledRSVP();
                cancelledRsvp.setRsvpId(rsvp.getId());
                cancelledRsvp.setUser(user);
                cancelledRsvp.setEvent(event);
                cancelledRsvp.setInitiatedBy("organiser");
                cancelledRsvp.setCancelledBy(organizer);
                cancelledRsvp.setPaymentStatus(rsvp.getPaymentStatus());
                cancelledRsvp.setAmountPaid(rsvp.getAmountPaid());
                cancelledRsvp.setStripePaymentIntentId(rsvp.getStripePaymentIntentId());
                cancelledRSVPRepository.save(cancelledRsvp);
            }

            // Delete associated payment records first to avoid FK constraint violation
            paymentRepository.deleteByRsvp(rsvp);

            // Delete RSVP
            rsvpRepository.delete(rsvp);
        }

        // Create block record
        BlockedRSVP blockedRSVP = new BlockedRSVP(event, user, organizer);
        blockedRSVPRepository.save(blockedRSVP);
    }

    /**
     * Unblocks a user from RSVPing to an event - ORGANIZER/ADMIN ONLY.
     * Removes the block but does not automatically recreate their RSVP.
     *
     * @param eventId     The event ID
     * @param userId      The user ID to unblock
     * @param organizerId The user ID of the requester (must be event organizer or admin)
     * @throws AccessDeniedException if user is not the event organizer or admin
     */
    @Transactional
    public void unblockUserFromEventAsOrganiser(Long eventId, Long userId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Verify the requester is the event organizer or an admin
        if (!event.getCreatedBy().getId().equals(organizerId) && !isUserAdmin(organizerId)) {
            throw new AccessDeniedException("Only the event organizer or admin can unblock users");
        }

        // Find and delete block record
        BlockedRSVP blockedRSVP = blockedRSVPRepository.findByEvent_IdAndUser_Id(eventId, userId)
                .orElseThrow(() -> new RuntimeException("User is not blocked from this event"));

        blockedRSVPRepository.delete(blockedRSVP);
    }

    /**
     * Check if a user is blocked from an event.
     *
     * @param eventId The event ID
     * @param userId  The user ID
     * @return true if user is blocked, false otherwise
     */
    public boolean isUserBlockedFromEvent(Long eventId, Long userId) {
        return blockedRSVPRepository.existsByEvent_IdAndUser_Id(eventId, userId);
    }

    public Optional<RSVP> findById(Long id) {
        return rsvpRepository.findById(id);
    }
}