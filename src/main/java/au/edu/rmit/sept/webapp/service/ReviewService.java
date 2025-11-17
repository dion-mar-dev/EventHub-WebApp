package au.edu.rmit.sept.webapp.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import au.edu.rmit.sept.webapp.dto.ReviewDTO;
import au.edu.rmit.sept.webapp.dto.DisplayReviewDTO;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Review;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import au.edu.rmit.sept.webapp.repository.ReviewRepository;

@Service
@Transactional
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository, EventRepository eventRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    public void saveReview(ReviewDTO reviewDTO, Long eventId, String username) {
        // Find the user and event
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        // TO DO: ADD VALIDATION

        // Create and save the new review
        Review review = new Review();
        review.setRating(reviewDTO.getRating());
        review.setComment(reviewDTO.getComment());
        review.setAuthor(author);
        review.setEvent(event);
        
        reviewRepository.save(review);
    }

    //Convert Review to DisplayReviewDTO
    private DisplayReviewDTO convertToDisplayDTO(Review review) {
        DisplayReviewDTO dto = new DisplayReviewDTO();
        dto.setAuthorUsername(review.getAuthor().getUsername());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());

        // TO DO IS VERIFIED CHECK
        // dto.setVerified(someLogicToCheckIfAttendee...);
        return dto;
    }
    
    //Get first five reviews for an event, sorted by createdAt descending
    @Transactional(readOnly = true)
    public List<DisplayReviewDTO> getRecentReviewsForEvent(Long eventId) {
        List<Review> reviews = reviewRepository.findTop5ByEventIdOrderByCreatedAtDesc(eventId);
        return reviews.stream()
                .map(this::convertToDisplayDTO)
                .collect(Collectors.toList());
    }

    //Get average rating for an event
    @Transactional(readOnly = true)
    public double getAverageRatingForEvent(Long eventId) {
        List<Review> reviews = reviewRepository.findByEventIdOrderByCreatedAtDesc(eventId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    //Get total number of reviews for an event
    @Transactional(readOnly = true)
    public long countReviewsForEvent(Long eventId) {
        // For efficiency, it would be better to add a `countByEventId` method to the repository.
        // But for simplicity, we can reuse the existing method.
        return reviewRepository.findByEventIdOrderByCreatedAtDesc(eventId).size();
    }
    
    
}
