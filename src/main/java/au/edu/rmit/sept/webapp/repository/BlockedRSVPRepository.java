package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.BlockedRSVP;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing blocked RSVPs.
 * Handles persistence operations for users blocked from events.
 */
@Repository
public interface BlockedRSVPRepository extends JpaRepository<BlockedRSVP, Long> {

    /**
     * Check if a user is blocked from an event.
     */
    boolean existsByEventAndUser(Event event, User user);

    /**
     * Check if a user is blocked from an event by IDs.
     */
    boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);

    /**
     * Find a specific block record.
     */
    Optional<BlockedRSVP> findByEventAndUser(Event event, User user);

    /**
     * Find a specific block record by IDs.
     */
    Optional<BlockedRSVP> findByEvent_IdAndUser_Id(Long eventId, Long userId);

    /**
     * Get all blocked users for an event with pagination.
     * Used by organizers to view blocked users in manageable pages.
     */
    @Query("SELECT b FROM BlockedRSVP b JOIN FETCH b.user WHERE b.event.id = :eventId ORDER BY b.blockedDate DESC")
    Page<BlockedRSVP> findByEventIdWithUsersPaginated(@Param("eventId") Long eventId, Pageable pageable);

    /**
     * Delete a block record by event and user IDs.
     * Used when unblocking a user.
     */
    void deleteByEvent_IdAndUser_Id(Long eventId, Long userId);

    /**
     * Count blocked users for an event.
     */
    Long countByEvent(Event event);
}
