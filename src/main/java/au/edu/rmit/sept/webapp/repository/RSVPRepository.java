// 📋 Missing Repository Methods to be implemented:

//   I'll need to add to RSVPRepository:
//   - countByEvent(Event event)
//   - existsByUserIdAndEventId(Long userId, Long eventId)
//   these are needed by EventService for event-card displaying

package au.edu.rmit.sept.webapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.model.User;

@Repository
public interface RSVPRepository extends JpaRepository<RSVP, Long> {

    // Count attendees for an event
    Long countByEvent(Event event);

    // Check if user has RSVP'd to an event
    boolean existsByUser_UsernameAndEvent_Id(String username, Long eventId);

    // Find specific RSVP for deletion
    Optional<RSVP> findByUser_UsernameAndEvent_Id(String username, Long eventId);

    // Alternative: find by user and event objects
    Optional<RSVP> findByUserAndEvent(User user, Event event);

    // Check if RSVP exists by user and event objects
    boolean existsByUserAndEvent(User user, Event event);

    // Find all RSVPs for an event with user data eagerly loaded
    // Using JOIN FETCH prevents N+1 queries. Ordering by rsvpDate DESC shows most
    // recent RSVPs first
    @Query("SELECT r FROM RSVP r JOIN FETCH r.user WHERE r.event.id = :eventId ORDER BY r.rsvpDate DESC")
    List<RSVP> findByEventIdWithUsers(@Param("eventId") Long eventId);

    /**
     * ADMIN PRIVILEGED METHOD - Delete all RSVPs for any event regardless of deactivation status.
     * For admin use only - can delete RSVPs for both active and deactivated events.
     */
    void deleteByEvent(Event event);

    /**
     * Delete RSVPs only for active events. Prevents deletion of RSVPs for deactivated events.
     * For regular users - only allows RSVP deletion for active events.
     */
    @Modifying
    @Query("DELETE FROM RSVP r WHERE r.event.id = :eventId AND r.event.deactivated = false")
    void deleteByActiveEvent(@Param("eventId") Long eventId);

    // Find all events that a user has RSVP'd to but didn't create (for My Events page)
    @Query("SELECT r.event FROM RSVP r WHERE r.user.id = :userId AND r.event.eventDate >= CURRENT_DATE AND r.event.createdBy.id != :userId ORDER BY r.event.eventDate ASC")
    List<Event> findUpcomingEventsByUserId(@Param("userId") Long userId);

    /**
     * Fetches RSVPs for an event with pagination support.
     * Used by organisers to view attendees in manageable pages.
     */
    @Query("SELECT r FROM RSVP r JOIN FETCH r.user WHERE r.event.id = :eventId ORDER BY r.rsvpDate DESC")
    Page<RSVP> findByEventIdWithUsersPaginated(@Param("eventId") Long eventId, Pageable pageable);

    /**
     * Deletes a specific RSVP by event and user.
     * Used when organisers cancel/block an attendee.
     */
    @Modifying
    @Query("DELETE FROM RSVP r WHERE r.event.id = :eventId AND r.user.id = :userId")
    void deleteByEventIdAndUserId(@Param("eventId") Long eventId, @Param("userId") Long userId);

    /**
     * Searches RSVPs for an event by username or email with pagination support.
     * Used by organisers to search attendees.
     */
    @Query("SELECT r FROM RSVP r JOIN FETCH r.user WHERE r.event.id = :eventId " +
           "AND (LOWER(r.user.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "     OR LOWER(r.user.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY r.rsvpDate DESC")
    Page<RSVP> searchEventAttendees(@Param("eventId") Long eventId,
                                     @Param("searchTerm") String searchTerm,
                                     Pageable pageable);

    /**
     * Counts total RSVPs for active (non-deactivated) events.
     */
    @Query("SELECT COUNT(r) FROM RSVP r WHERE r.event.deactivated = false")
    long countActiveRsvps();
}