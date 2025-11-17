package au.edu.rmit.sept.webapp.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;

public class EventCreateDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    @NotNull(message = "Event date is required")
    @FutureOrPresent(message = "Event date must be today or in the future")
    private LocalDate eventDate;

    @NotNull(message = "Event time is required")
    private LocalTime eventTime;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity; // null = unlimited

    @NotNull(message = "Please select a category")
    private Long categoryId;

    private boolean unlimitedCapacity = false;

    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    private Set<Long> keywordIds;
    private List<String> customKeywords;

    // Default constructor
    public EventCreateDTO() {
    }

    // Constructor with parameters
    public EventCreateDTO(String title, String description, LocalDate eventDate, LocalTime eventTime,
            String location, Integer capacity, Long categoryId, boolean unlimitedCapacity) {
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.location = location;
        this.capacity = capacity;
        this.categoryId = categoryId;
        this.unlimitedCapacity = unlimitedCapacity;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public boolean isUnlimitedCapacity() {
        return unlimitedCapacity;
    }

    public void setUnlimitedCapacity(boolean unlimitedCapacity) {
        this.unlimitedCapacity = unlimitedCapacity;
    }

    public Set<Long> getKeywordIds() {
        return keywordIds;
    }

    public void setKeywordIds(Set<Long> keywordIds) {
        this.keywordIds = keywordIds;
    }

    public List<String> getCustomKeywords() {
        return customKeywords;
    }

    public void setCustomKeywords(List<String> customKeywords) {
        this.customKeywords = customKeywords;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    // Helper method to get combined date/time
    public LocalDateTime getEventDateTime() {
        if (eventDate != null && eventTime != null) {
            return LocalDateTime.of(eventDate, eventTime);
        }
        return null;
    }

    // Custom validation method to check if event is in the future
    @AssertTrue(message = "Event date and time must be in the future")
    public boolean isEventInFuture() {
        LocalDateTime eventDateTime = getEventDateTime();
        if (eventDateTime == null) {
            return true; // Let other validations handle null values
        }
        // Allow events that are at least 5 minutes in the future to account for processing time
        // This prevents race conditions during testing and normal operation
        return eventDateTime.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    // Custom validation for capacity
    @AssertTrue(message = "Please specify capacity or select unlimited")
    public boolean isCapacityValid() {
        // If unlimited is checked, capacity can be null
        // If unlimited is not checked, capacity must be provided
        return unlimitedCapacity || capacity != null;
    }
}