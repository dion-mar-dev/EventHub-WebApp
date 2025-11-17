package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.Payment;
import au.edu.rmit.sept.webapp.model.RSVP;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Delete all payment records associated with an RSVP.
     * Used before deleting an RSVP to avoid FK constraint violations.
     */
    void deleteByRsvp(RSVP rsvp);
}