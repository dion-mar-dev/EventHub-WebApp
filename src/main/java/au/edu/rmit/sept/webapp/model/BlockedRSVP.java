package au.edu.rmit.sept.webapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to track users who have been blocked from RSVPing to specific events.
 * When a user is blocked, their RSVP is deleted and they cannot RSVP again
 * until unblocked by the event organizer or admin.
 */
@Entity
@Table(name = "blocked_rsvps",
       uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
public class BlockedRSVP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "blocked_date", nullable = false, updatable = false)
    private LocalDateTime blockedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_id", nullable = false)
    private User blockedBy;

    // Default constructor
    public BlockedRSVP() {
    }

    // Constructor for creating a new block record
    public BlockedRSVP(Event event, User user, User blockedBy) {
        this.event = event;
        this.user = user;
        this.blockedBy = blockedBy;
        this.blockedDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getBlockedDate() {
        return blockedDate;
    }

    public void setBlockedDate(LocalDateTime blockedDate) {
        this.blockedDate = blockedDate;
    }

    public User getBlockedBy() {
        return blockedBy;
    }

    public void setBlockedBy(User blockedBy) {
        this.blockedBy = blockedBy;
    }
}
