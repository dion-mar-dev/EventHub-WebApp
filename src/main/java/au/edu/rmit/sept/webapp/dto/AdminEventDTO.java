package au.edu.rmit.sept.webapp.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class AdminEventDTO {
    private Long eventId;
    private String title;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String organizerUsername;
    private Long rsvpCount;
    private String categoryName;
    private boolean deactivated;
    private String deactivatedByUsername;

    public AdminEventDTO() {
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

    public String getOrganizerUsername() {
        return organizerUsername;
    }

    public void setOrganizerUsername(String organizerUsername) {
        this.organizerUsername = organizerUsername;
    }

    public Long getRsvpCount() {
        return rsvpCount;
    }

    public void setRsvpCount(Long rsvpCount) {
        this.rsvpCount = rsvpCount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public boolean isDeactivated() {
        return deactivated;
    }

    public void setDeactivated(boolean deactivated) {
        this.deactivated = deactivated;
    }

    public String getDeactivatedByUsername() {
        return deactivatedByUsername;
    }

    public void setDeactivatedByUsername(String deactivatedByUsername) {
        this.deactivatedByUsername = deactivatedByUsername;
    }
}