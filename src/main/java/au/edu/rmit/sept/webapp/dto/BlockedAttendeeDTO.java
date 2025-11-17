package au.edu.rmit.sept.webapp.dto;

import java.time.LocalDateTime;

/**
 * DTO for representing blocked attendees in the UI.
 * Contains user information and blocking details.
 */
public class BlockedAttendeeDTO {
    private Long userId;
    private String username;
    private String email;
    private LocalDateTime blockedDate;
    private String blockedBy;  // Username of the organizer/admin who blocked them

    // Default constructor
    public BlockedAttendeeDTO() {
    }

    // Constructor with parameters
    public BlockedAttendeeDTO(Long userId, String username, String email, LocalDateTime blockedDate, String blockedBy) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.blockedDate = blockedDate;
        this.blockedBy = blockedBy;
    }

    // Getters and setters
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getBlockedDate() {
        return blockedDate;
    }

    public void setBlockedDate(LocalDateTime blockedDate) {
        this.blockedDate = blockedDate;
    }

    public String getBlockedBy() {
        return blockedBy;
    }

    public void setBlockedBy(String blockedBy) {
        this.blockedBy = blockedBy;
    }
}
