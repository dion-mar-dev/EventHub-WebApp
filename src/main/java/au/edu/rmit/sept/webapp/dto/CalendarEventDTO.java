package au.edu.rmit.sept.webapp.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for calendar events.
 * Used to transfer event data to the frontend calendar component.
 */
public class CalendarEventDTO {
    private Long id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private String color;
    private String categoryName;
    private String location;
    private String description;
    private boolean attending;

    public CalendarEventDTO() {
    }

    public CalendarEventDTO(Long id, String title, LocalDateTime start, LocalDateTime end, 
                           String color, String categoryName, String location, 
                           String description, boolean attending) {
        this.id = id;
        this.title = title;
        this.start = start;
        this.end = end;
        this.color = color;
        this.categoryName = categoryName;
        this.location = location;
        this.description = description;
        this.attending = attending;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAttending() {
        return attending;
    }

    public void setAttending(boolean attending) {
        this.attending = attending;
    }
}
