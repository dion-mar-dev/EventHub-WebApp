package au.edu.rmit.sept.webapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import au.edu.rmit.sept.webapp.dto.KeywordDTO;

/**
 * DTO for displaying complete event details on the view page.
 * Unlike EventCardDTO which truncates descriptions for card displays,
 * this DTO contains full event information and supports future additions
 * like attendee lists and creator details.
 */
public class EventDetailsDTO {

    // Core event fields
    private Long eventId;
    private String title;
    private String fullDescription; // Complete description, no truncation
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String location;

    // Category information
    private String categoryName;
    private String categoryColor;

    // Capacity and attendance
    private Integer attendeeCount;
    private Integer maxAttendees; // null = unlimited
    private boolean isEventFull;
    private boolean isEventStarted;

    // Payment information
    private BigDecimal price; // null = free event
    private boolean requiresPayment; // true if event requires payment

    // User-specific data
    private boolean userRsvpStatus;
    private boolean userBlockedStatus; // Indicates if user is blocked from RSVPing
    private String userPaymentStatus; // "pending", "paid", or null
    private Long userRsvpId; // needed for payment form

    // Creator information
    private String createdByUsername;
    private Long createdById;
    private LocalDateTime createdAt;

    // Future expansion - attendee list
    private List<AttendeeDTO> attendees = new ArrayList<>();

    private List<KeywordDTO> keywords;

    // Default constructor
    public EventDetailsDTO() {
    }

    // Getters and Setters
    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }

    public Integer getAttendeeCount() {
        return attendeeCount;
    }

    public void setAttendeeCount(Integer attendeeCount) {
        this.attendeeCount = attendeeCount;
    }

    public Integer getMaxAttendees() {
        return maxAttendees;
    }

    public void setMaxAttendees(Integer maxAttendees) {
        this.maxAttendees = maxAttendees;
    }

    public boolean isEventFull() {
        return isEventFull;
    }

    public void setEventFull(boolean eventFull) {
        isEventFull = eventFull;
    }

    public boolean isEventStarted() {
        return isEventStarted;
    }

    public void setEventStarted(boolean eventStarted) {
        isEventStarted = eventStarted;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isRequiresPayment() {
        return requiresPayment;
    }

    public void setRequiresPayment(boolean requiresPayment) {
        this.requiresPayment = requiresPayment;
    }

    public boolean isUserRsvpStatus() {
        return userRsvpStatus;
    }

    public void setUserRsvpStatus(boolean userRsvpStatus) {
        this.userRsvpStatus = userRsvpStatus;
    }

    public boolean isUserBlockedStatus() {
        return userBlockedStatus;
    }

    public void setUserBlockedStatus(boolean userBlockedStatus) {
        this.userBlockedStatus = userBlockedStatus;
    }

    public String getUserPaymentStatus() {
        return userPaymentStatus;
    }

    public void setUserPaymentStatus(String userPaymentStatus) {
        this.userPaymentStatus = userPaymentStatus;
    }

    public Long getUserRsvpId() {
        return userRsvpId;
    }

    public void setUserRsvpId(Long userRsvpId) {
        this.userRsvpId = userRsvpId;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<KeywordDTO> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<KeywordDTO> keywords) {
        this.keywords = keywords;
    }

    public List<AttendeeDTO> getAttendees() {
        return attendees;
    }

    public void setAttendees(List<AttendeeDTO> attendees) {
        this.attendees = attendees;
    }

    // Helper method for combined date/time
    public LocalDateTime getEventDateTime() {
        if (eventDate != null && eventTime != null) {
            return LocalDateTime.of(eventDate, eventTime);
        }
        return null;
    }

    /**
     * Inner class for attendee information.
     * Can be expanded later to include profile pictures, etc.
     */
    public static class AttendeeDTO {
        private Long userId;
        private String username;
        private LocalDateTime rsvpDate;

        public AttendeeDTO() {
        }

        public AttendeeDTO(Long userId, String username, LocalDateTime rsvpDate) {
            this.userId = userId;
            this.username = username;
            this.rsvpDate = rsvpDate;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public LocalDateTime getRsvpDate() {
            return rsvpDate;
        }

        public void setRsvpDate(LocalDateTime rsvpDate) {
            this.rsvpDate = rsvpDate;
        }
    }
}