package au.edu.rmit.sept.webapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CancelledRSVPDTO {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private LocalDateTime cancelledAt;
    private String initiatedBy; // "admin", "organiser", "attendee"
    private String displayBadge; // "Blocked", "Cancelled - Self", "Cancelled - Organiser"
    private String paymentStatus;
    private BigDecimal amountPaid;
    private String refundStatus; // null, "refunded", "failed"
    private LocalDateTime refundedAt;

    // Constructor
    public CancelledRSVPDTO(Long id, Long userId, String username, String email,
                           LocalDateTime cancelledAt, String initiatedBy,
                           String paymentStatus, BigDecimal amountPaid,
                           String refundStatus, LocalDateTime refundedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.cancelledAt = cancelledAt;
        this.initiatedBy = initiatedBy;
        this.paymentStatus = paymentStatus;
        this.amountPaid = amountPaid;
        this.refundStatus = refundStatus;
        this.refundedAt = refundedAt;

        // Generate display badge based on initiated_by
        this.displayBadge = generateDisplayBadge(initiatedBy);
    }

    private String generateDisplayBadge(String initiatedBy) {
        switch (initiatedBy) {
            case "attendee":
                return "Cancelled - Self";
            case "organiser":
                return "Cancelled - Organiser";
            case "admin":
                return "Cancelled - Admin";
            default:
                return "Cancelled";
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public String getDisplayBadge() {
        return displayBadge;
    }

    public void setDisplayBadge(String displayBadge) {
        this.displayBadge = displayBadge;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }
}
