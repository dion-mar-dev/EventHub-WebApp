package au.edu.rmit.sept.webapp.dto;

import java.time.LocalDateTime;

public class AdminUserDTO {
    
    private Long userId;
    private String username;
    private String email;
    private LocalDateTime joinDate;
    private Long eventsCreatedCount;
    private Long rsvpsMadeCount;
    private boolean deactivated;
    private String deactivatedByUsername;

    public AdminUserDTO() {}

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

    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDateTime joinDate) {
        this.joinDate = joinDate;
    }

    public Long getEventsCreatedCount() {
        return eventsCreatedCount;
    }

    public void setEventsCreatedCount(Long eventsCreatedCount) {
        this.eventsCreatedCount = eventsCreatedCount;
    }

    public Long getRsvpsMadeCount() {
        return rsvpsMadeCount;
    }

    public void setRsvpsMadeCount(Long rsvpsMadeCount) {
        this.rsvpsMadeCount = rsvpsMadeCount;
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