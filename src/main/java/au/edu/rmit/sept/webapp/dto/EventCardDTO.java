package au.edu.rmit.sept.webapp.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/*
 * Note: EventService class actually populates the EventCardDTO.
 * Currently EventService not implemented
 */

public class EventCardDTO {
    
    private Long eventId;
    private String title;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String location;
    private String briefDescription; // Service truncates Event.description to 50 chars
    private String description; // Full description up to 100 chars for full mode
    private String categoryName;
    private String categoryColor;
    private String creatorUsername;
    private Integer attendeeCount; // Service calculates via RSVPRepository.countByEvent()
    private Integer maxAttendees;  // null = unlimited
    private boolean userRsvpStatus;
    private boolean isEventStarted;
    private boolean isEventFull;
    private boolean isOrganiser;
    private List<KeywordDTO> keywords;
    
    public EventCardDTO() {}
    
    public EventCardDTO(Long eventId, String title, LocalDate eventDate, LocalTime eventTime, 
                       String location, String briefDescription, String description, String categoryName, 
                       String categoryColor, String creatorUsername, Integer attendeeCount, Integer maxAttendees,
                       boolean userRsvpStatus, boolean isEventStarted, boolean isEventFull, boolean isOrganiser) {
        this.eventId = eventId;
        this.title = title;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.location = location;
        this.briefDescription = briefDescription;
        this.description = description;
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
        this.creatorUsername = creatorUsername;
        this.attendeeCount = attendeeCount;
        this.maxAttendees = maxAttendees;
        this.userRsvpStatus = userRsvpStatus;
        this.isEventStarted = isEventStarted;
        this.isEventFull = isEventFull;
        this.isOrganiser = isOrganiser;
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
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getBriefDescription() {
        return briefDescription;
    }
    
    public void setBriefDescription(String briefDescription) {
        this.briefDescription = briefDescription;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public boolean isUserRsvpStatus() {
        return userRsvpStatus;
    }
    
    public void setUserRsvpStatus(boolean userRsvpStatus) {
        this.userRsvpStatus = userRsvpStatus;
    }
    
    public boolean isEventStarted() {
        return isEventStarted;
    }
    
    public void setEventStarted(boolean isEventStarted) {
        this.isEventStarted = isEventStarted;
    }
    
    public boolean isEventFull() {
        return isEventFull;
    }
    
    public void setEventFull(boolean isEventFull) {
        this.isEventFull = isEventFull;
    }
    
    // Add getter/setter
    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }
    
    public boolean isOrganiser() {
        return isOrganiser;
    }
    
    public void setOrganiser(boolean isOrganiser) {
        this.isOrganiser = isOrganiser;
    }
    
    public List<KeywordDTO> getKeywords() {
        return keywords;
    }
    
    public void setKeywords(List<KeywordDTO> keywords) {
        this.keywords = keywords;
    }
}
