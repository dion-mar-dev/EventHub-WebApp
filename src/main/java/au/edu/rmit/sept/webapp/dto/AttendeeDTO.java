package au.edu.rmit.sept.webapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AttendeeDTO {
    private Long userId;
    private String username;
    private String email;
    private LocalDateTime rsvpDate;
    private BigDecimal eventPrice;
    private String paymentStatus;

    public AttendeeDTO(Long userId, String username, String email, LocalDateTime rsvpDate) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.rsvpDate = rsvpDate;
    }

    public AttendeeDTO(Long userId, String username, String email, LocalDateTime rsvpDate, BigDecimal eventPrice, String paymentStatus) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.rsvpDate = rsvpDate;
        this.eventPrice = eventPrice;
        this.paymentStatus = paymentStatus;
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

    public LocalDateTime getRsvpDate() {
        return rsvpDate;
    }

    public void setRsvpDate(LocalDateTime rsvpDate) {
        this.rsvpDate = rsvpDate;
    }

    public BigDecimal getEventPrice() {
        return eventPrice;
    }

    public void setEventPrice(BigDecimal eventPrice) {
        this.eventPrice = eventPrice;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}