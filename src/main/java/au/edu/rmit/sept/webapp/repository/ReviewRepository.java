package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.model.User;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Find ALL reviews for an event, ordered by creation date descending, for when the, "view all reviews" button is pressed
    List<Review> findByEventIdOrderByCreatedAtDesc(Long eventId);

    // Find newest 5 reviews for an event, for the quick view on event detailed view
    List<Review> findTop5ByEventIdOrderByCreatedAtDesc(Long eventId);

}