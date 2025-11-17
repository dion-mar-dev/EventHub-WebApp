package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.CancelledRSVP;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CancelledRSVPRepository extends JpaRepository<CancelledRSVP, Long> {

    /**
     * Get all cancelled RSVPs for an event (paginated)
     */
    @Query("SELECT cr FROM CancelledRSVP cr " +
           "JOIN FETCH cr.user " +
           "WHERE cr.event.id = :eventId " +
           "ORDER BY cr.cancelledAt DESC")
    Page<CancelledRSVP> findByEventIdWithUsersPaginated(@Param("eventId") Long eventId, Pageable pageable);

    /**
     * Count cancelled RSVPs for an event
     */
    long countByEvent_Id(Long eventId);

    /**
     * Find cancelled RSVPs that need refunds (paid but not refunded)
     */
    @Query("SELECT cr FROM CancelledRSVP cr " +
           "WHERE cr.event.id = :eventId " +
           "AND cr.paymentStatus = 'paid' " +
           "AND cr.refundStatus IS NULL")
    Page<CancelledRSVP> findPendingRefundsByEventId(@Param("eventId") Long eventId, Pageable pageable);
}
