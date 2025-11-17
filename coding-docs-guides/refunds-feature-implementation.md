# Refunds Feature Implementation Plan

## Overview

Implement a "Refunds (Cancelled/Blocked RSVPs)" tab in the organizer's attendees management modal that allows organizers to refund payments for cancelled or blocked attendees. This feature only applies to paid events and provides full audit trail of all cancellations.

---

## Requirements Summary

### Core Functionality
- Track all cancelled RSVPs (user-initiated, organizer-initiated, and blocked users)
- Allow organizers to refund payments via Stripe API
- Maintain complete audit trail of cancellations and refunds
- Show cancelled/blocked users in dedicated tab (paid events only)
- Display refund status with visual badges

### Business Rules
1. **Free events**: No "Refunds" tab needed - cancellations are simple deletions
2. **Paid events**: All cancellations tracked in `cancelled_rsvps` table
3. **Blocked users**: Appear in BOTH "Blocked Users" tab AND "Refunds" tab (if paid event)
4. **Re-RSVP after cancellation**: Historical cancelled record stays in archive, new RSVP is separate
5. **Refund control**: Organizer has explicit control - no automatic refunds

### User Experience
- **Tab naming**: "Refunds (Cancelled/Blocked RSVPs)"
- **Refund confirmation**: Dialog shows amount and username before processing
- **Refund button states**:
  - Before: Orange "Refund Payment" button
  - After: Green "Refunded ✓" badge (disabled)
  - Failed: Red "Refund Failed" with retry option
- **Audit visibility**: All cancelled records remain visible for history

---

## Database Layer

### New Table: `cancelled_rsvps`

```sql
CREATE TABLE IF NOT EXISTS cancelled_rsvps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Original RSVP reference (FK to rsvp table, can be null if RSVP already deleted)
    rsvp_id BIGINT,

    -- User and Event references (required for audit even if RSVP deleted)
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,

    -- Cancellation metadata
    cancelled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    initiated_by VARCHAR(20) NOT NULL, -- 'admin', 'organiser', or 'attendee'
    cancelled_by_user_id BIGINT NOT NULL, -- Who performed the cancellation action

    -- Payment information (copied from original RSVP at time of cancellation)
    payment_status VARCHAR(20), -- 'paid', 'pending', or null (for free events)
    amount_paid DECIMAL(10,2),
    stripe_payment_intent_id VARCHAR(255),

    -- Refund tracking
    refund_status VARCHAR(20), -- null, 'refunded', or 'failed'
    refunded_at TIMESTAMP,
    stripe_refund_id VARCHAR(255),
    refunded_by_user_id BIGINT, -- Who processed the refund

    -- Foreign keys
    CONSTRAINT fk_cancelled_rsvp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cancelled_rsvp_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_cancelled_by_user FOREIGN KEY (cancelled_by_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_refunded_by_user FOREIGN KEY (refunded_by_user_id) REFERENCES users(id) ON DELETE SET NULL,

    -- Indexes for performance
    INDEX idx_cancelled_rsvps_event_id (event_id),
    INDEX idx_cancelled_rsvps_user_id (user_id),
    INDEX idx_cancelled_rsvps_refund_status (refund_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Schema Notes
- `rsvp_id` can be null because original RSVP is deleted, but we still track user/event
- `payment_status`, `amount_paid`, and `stripe_payment_intent_id` are copied from RSVP at cancellation time
- `initiated_by` distinguishes between user self-cancellation, organizer cancellation, and admin actions
- `refund_status` null means no refund attempted yet (could be pending payment or organizer chose not to refund)

---

## Backend Layer

### 1. Model: `CancelledRSVP.java`

**Location**: `src/main/java/au/edu/rmit/sept/webapp/model/CancelledRSVP.java`

```java
package au.edu.rmit.sept.webapp.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancelled_rsvps")
public class CancelledRSVP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rsvp_id")
    private Long rsvpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @CreationTimestamp
    @Column(name = "cancelled_at", nullable = false, updatable = false)
    private LocalDateTime cancelledAt;

    @Column(name = "initiated_by", nullable = false, length = 20)
    private String initiatedBy; // "admin", "organiser", "attendee"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id", nullable = false)
    private User cancelledBy;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    @Column(name = "refund_status", length = 20)
    private String refundStatus;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "stripe_refund_id", length = 255)
    private String stripeRefundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refunded_by_user_id")
    private User refundedBy;

    // Constructors, getters, setters...
}
```

### 2. Repository: `CancelledRSVPRepository.java`

**Location**: `src/main/java/au/edu/rmit/sept/webapp/repository/CancelledRSVPRepository.java`

```java
package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.CancelledRSVP;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CancelledRSVPRepository extends JpaRepository<CancelledRSVP, Long> {

    // Get all cancelled RSVPs for an event (paginated)
    @Query("SELECT cr FROM CancelledRSVP cr " +
           "JOIN FETCH cr.user " +
           "WHERE cr.event.id = :eventId " +
           "ORDER BY cr.cancelledAt DESC")
    Page<CancelledRSVP> findByEventIdWithUsersPaginated(@Param("eventId") Long eventId, Pageable pageable);

    // Count cancelled RSVPs for an event
    long countByEvent_Id(Long eventId);

    // Find cancelled RSVPs that need refunds (paid but not refunded)
    @Query("SELECT cr FROM CancelledRSVP cr " +
           "WHERE cr.event.id = :eventId " +
           "AND cr.paymentStatus = 'paid' " +
           "AND cr.refundStatus IS NULL")
    Page<CancelledRSVP> findPendingRefundsByEventId(@Param("eventId") Long eventId, Pageable pageable);
}
```

### 3. DTO: `CancelledRSVPDTO.java`

**Location**: `src/main/java/au/edu/rmit/sept/webapp/dto/CancelledRSVPDTO.java`

```java
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

    // Getters and setters...
}
```

### 4. Service: `StripeService.java` (Add Refund Method)

**Location**: `src/main/java/au/edu/rmit/sept/webapp/service/StripeService.java`

**Add this method:**

```java
/**
 * Processes a refund for a Stripe payment.
 *
 * @param paymentIntentId The Stripe payment intent ID to refund
 * @param amount The amount to refund (in dollars)
 * @return The Stripe refund ID
 * @throws StripeException if refund fails
 */
public String refundPayment(String paymentIntentId, BigDecimal amount) throws StripeException {
    // Convert amount to cents
    long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

    // Create refund parameters
    RefundCreateParams params = RefundCreateParams.builder()
            .setPaymentIntent(paymentIntentId)
            .setAmount(amountInCents)
            .build();

    // Process refund via Stripe API
    Refund refund = Refund.create(params);

    return refund.getId();
}
```

### 5. Service: `RSVPService.java` (Update Existing Methods)

**Location**: `src/main/java/au/edu/rmit/sept/webapp/service/RSVPService.java`

**Inject CancelledRSVPRepository:**
```java
private final CancelledRSVPRepository cancelledRSVPRepository;
```

**Update `cancelRSVP()` method:**
```java
public void cancelRSVP(Long eventId, String username) {
    // Find RSVP
    RSVP rsvp = rsvpRepository.findByUser_UsernameAndEvent_Id(username, eventId)
            .orElseThrow(() -> new RuntimeException("RSVP not found"));

    // If paid event, create cancelled RSVP record
    if (rsvp.getEvent().getRequiresPayment() && rsvp.getPaymentStatus() != null) {
        CancelledRSVP cancelledRsvp = new CancelledRSVP();
        cancelledRsvp.setRsvpId(rsvp.getId());
        cancelledRsvp.setUser(rsvp.getUser());
        cancelledRsvp.setEvent(rsvp.getEvent());
        cancelledRsvp.setInitiatedBy("attendee");
        cancelledRsvp.setCancelledBy(rsvp.getUser()); // User cancelled their own RSVP
        cancelledRsvp.setPaymentStatus(rsvp.getPaymentStatus());
        cancelledRsvp.setAmountPaid(rsvp.getAmountPaid());
        cancelledRsvp.setStripePaymentIntentId(rsvp.getStripePaymentIntentId());
        cancelledRSVPRepository.save(cancelledRsvp);
    }

    // Delete RSVP
    rsvpRepository.delete(rsvp);
}
```

**Update `blockUserFromEventAsOrganiser()` method:**
```java
@Transactional
public void blockUserFromEventAsOrganiser(Long eventId, Long userId, Long organizerId) {
    Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    User organizer = userRepository.findById(organizerId)
            .orElseThrow(() -> new RuntimeException("Organizer not found"));

    // Verify organizer permissions
    if (!event.getCreatedBy().getId().equals(organizerId) && !isUserAdmin(organizerId)) {
        throw new AccessDeniedException("Only the event organizer or admin can block users");
    }

    // Check if already blocked
    if (blockedRSVPRepository.existsByEventAndUser(event, user)) {
        throw new RuntimeException("User is already blocked from this event");
    }

    // If user has RSVP, create cancelled record before deleting
    Optional<RSVP> existingRsvp = rsvpRepository.findByUserAndEvent(user, event);
    if (existingRsvp.isPresent() && event.getRequiresPayment()) {
        RSVP rsvp = existingRsvp.get();

        CancelledRSVP cancelledRsvp = new CancelledRSVP();
        cancelledRsvp.setRsvpId(rsvp.getId());
        cancelledRsvp.setUser(user);
        cancelledRsvp.setEvent(event);
        cancelledRsvp.setInitiatedBy("organiser");
        cancelledRsvp.setCancelledBy(organizer);
        cancelledRsvp.setPaymentStatus(rsvp.getPaymentStatus());
        cancelledRsvp.setAmountPaid(rsvp.getAmountPaid());
        cancelledRsvp.setStripePaymentIntentId(rsvp.getStripePaymentIntentId());
        cancelledRSVPRepository.save(cancelledRsvp);
    }

    // Delete existing RSVP
    rsvpRepository.deleteByEventIdAndUserId(eventId, userId);

    // Create block record
    BlockedRSVP blockedRSVP = new BlockedRSVP(event, user, organizer);
    blockedRSVPRepository.save(blockedRSVP);
}
```

### 6. Service: `EventService.java` (Add New Methods)

**Location**: `src/main/java/au/edu/rmit/sept/webapp/service/EventService.java`

**Inject repositories:**
```java
private final CancelledRSVPRepository cancelledRSVPRepository;
private final StripeService stripeService;
```

**Add methods:**

```java
/**
 * Get cancelled RSVPs for an event - ORGANISER ONLY.
 */
@Transactional(readOnly = true)
public Page<CancelledRSVPDTO> getCancelledRSVPs(Long eventId, Long userId, Pageable pageable) {
    // Verify user is organizer or admin
    Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("Event not found"));

    if (!event.getCreatedBy().getId().equals(userId) && !userService.hasRole(userId, "ROLE_ADMIN")) {
        throw new AccessDeniedException("Only the event organizer or admin can view cancelled RSVPs");
    }

    // Fetch cancelled RSVPs
    Page<CancelledRSVP> cancelledRsvps = cancelledRSVPRepository
            .findByEventIdWithUsersPaginated(eventId, pageable);

    // Convert to DTOs
    List<CancelledRSVPDTO> dtos = cancelledRsvps.getContent().stream()
            .map(cr -> new CancelledRSVPDTO(
                    cr.getId(),
                    cr.getUser().getId(),
                    cr.getUser().getUsername(),
                    cr.getUser().getEmail(),
                    cr.getCancelledAt(),
                    cr.getInitiatedBy(),
                    cr.getPaymentStatus(),
                    cr.getAmountPaid(),
                    cr.getRefundStatus(),
                    cr.getRefundedAt()))
            .collect(Collectors.toList());

    return new PageImpl<>(dtos, pageable, cancelledRsvps.getTotalElements());
}

/**
 * Process refund for a cancelled RSVP - ORGANISER ONLY.
 */
@Transactional
public void refundCancelledRSVP(Long cancelledRsvpId, Long organizerId) {
    // Find cancelled RSVP
    CancelledRSVP cancelledRsvp = cancelledRSVPRepository.findById(cancelledRsvpId)
            .orElseThrow(() -> new EntityNotFoundException("Cancelled RSVP not found"));

    Event event = cancelledRsvp.getEvent();

    // Verify user is organizer or admin
    if (!event.getCreatedBy().getId().equals(organizerId) && !userService.hasRole(organizerId, "ROLE_ADMIN")) {
        throw new AccessDeniedException("Only the event organizer or admin can process refunds");
    }

    // Verify payment was made
    if (cancelledRsvp.getPaymentStatus() == null || !cancelledRsvp.getPaymentStatus().equals("paid")) {
        throw new IllegalStateException("Cannot refund - no payment was made");
    }

    // Verify not already refunded
    if (cancelledRsvp.getRefundStatus() != null && cancelledRsvp.getRefundStatus().equals("refunded")) {
        throw new IllegalStateException("Payment has already been refunded");
    }

    // Process refund via Stripe
    try {
        String refundId = stripeService.refundPayment(
                cancelledRsvp.getStripePaymentIntentId(),
                cancelledRsvp.getAmountPaid());

        // Update cancelled RSVP record
        cancelledRsvp.setRefundStatus("refunded");
        cancelledRsvp.setRefundedAt(LocalDateTime.now());
        cancelledRsvp.setStripeRefundId(refundId);
        cancelledRsvp.setRefundedBy(userRepository.findById(organizerId).orElse(null));
        cancelledRSVPRepository.save(cancelledRsvp);

    } catch (StripeException e) {
        // Mark as failed
        cancelledRsvp.setRefundStatus("failed");
        cancelledRSVPRepository.save(cancelledRsvp);
        throw new RuntimeException("Refund failed: " + e.getMessage());
    }
}
```

### 7. Controller: `EventApiController.java` (Add Endpoints)

**Location**: `src/main/java/au/edu/rmit/sept/webapp/controller/EventApiController.java`

**Add endpoints:**

```java
/**
 * Fetches paginated cancelled RSVPs for an event - ORGANISER ONLY.
 */
@GetMapping("/{eventId}/cancelled-rsvps")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> getCancelledRSVPs(
        @PathVariable Long eventId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication) {

    try {
        Long userId = userService.getUserIdByUsername(authentication.getName());
        Page<CancelledRSVPDTO> cancelledRsvps = eventService.getCancelledRSVPs(
                eventId, userId, PageRequest.of(page, size));

        Map<String, Object> response = new HashMap<>();
        response.put("cancelledRsvps", cancelledRsvps.getContent());
        response.put("currentPage", cancelledRsvps.getNumber());
        response.put("totalPages", cancelledRsvps.getTotalPages());
        response.put("totalElements", cancelledRsvps.getTotalElements());

        return ResponseEntity.ok(response);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

/**
 * Process refund for a cancelled RSVP - ORGANISER ONLY.
 */
@PostMapping("/{eventId}/cancelled-rsvps/{cancelledRsvpId}/refund")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> refundCancelledRSVP(
        @PathVariable Long eventId,
        @PathVariable Long cancelledRsvpId,
        Authentication authentication) {

    try {
        Long userId = userService.getUserIdByUsername(authentication.getName());
        eventService.refundCancelledRSVP(cancelledRsvpId, userId);

        return ResponseEntity.ok(Map.of("success", true,
                "message", "Refund processed successfully"));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```

---

## Frontend Layer

### 1. HTML: Add "Refunds" Tab to Attendees Modal

**Location**: `src/main/resources/templates/events/event-details.html`

**Find the attendees modal tab navigation** (around line 722) and add new tab:

```html
<!-- Tab Navigation -->
<ul class="nav nav-tabs mb-3" id="attendeesTabs" role="tablist">
    <li class="nav-item" role="presentation">
        <button class="nav-link active" id="rsvps-tab" data-bs-toggle="tab"
                data-bs-target="#rsvps-pane" type="button" role="tab">
            <i class="fas fa-calendar-check me-2"></i>RSVPs
            <span class="badge bg-secondary ms-2" id="rsvpCount">0</span>
        </button>
    </li>
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="blocked-tab" data-bs-toggle="tab"
                data-bs-target="#blocked-pane" type="button" role="tab">
            <i class="fas fa-ban me-2"></i>Blocked Users
            <span class="badge bg-danger ms-2" id="blockedCount">0</span>
        </button>
    </li>
    <!-- NEW TAB: Only show for paid events -->
    <li class="nav-item" role="presentation" th:if="${event.requiresPayment}">
        <button class="nav-link" id="refunds-tab" data-bs-toggle="tab"
                data-bs-target="#refunds-pane" type="button" role="tab">
            <i class="fas fa-undo me-2"></i>Refunds (Cancelled/Blocked)
            <span class="badge bg-warning ms-2" id="refundsCount">0</span>
        </button>
    </li>
</ul>
```

### 2. HTML: Add Refunds Tab Content Pane

**Add after the "Blocked Users" tab pane** (around line 840):

```html
<!-- Refunds Tab -->
<div class="tab-pane fade" id="refunds-pane" role="tabpanel" th:if="${event.requiresPayment}">
    <!-- Loading spinner -->
    <div id="refundsLoading" class="text-center py-5">
        <div class="spinner-border text-warning" role="status">
            <span class="visually-hidden">Loading...</span>
        </div>
    </div>

    <!-- Refunds table (hidden initially) -->
    <div id="refundsContent" style="display: none;">
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>Username</th>
                        <th>Email</th>
                        <th>Cancelled Date</th>
                        <th>Type</th>
                        <th>Amount</th>
                        <th>Refund Status</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody id="refundsTableBody">
                    <!-- Populated via JavaScript -->
                </tbody>
            </table>
        </div>

        <!-- Pagination controls -->
        <nav id="refundsPagination">
            <ul class="pagination justify-content-center">
                <!-- Populated via JavaScript -->
            </ul>
        </nav>
    </div>

    <!-- Empty state -->
    <div id="refundsEmpty" class="text-center py-5" style="display: none;">
        <i class="fas fa-hand-holding-usd fa-3x text-muted mb-3"></i>
        <p class="text-muted">No cancelled RSVPs requiring refunds</p>
    </div>
</div>
```

### 3. JavaScript: Add Refunds Functions

**Location**: `src/main/resources/templates/events/event-details.html` (scripts section)

**Add after existing JavaScript functions** (around line 1200):

```javascript
// Refunds Tab Variables
let currentRefundsPage = 0;
let totalRefundsPages = 0;

/**
 * Load cancelled RSVPs for refunds tab
 */
function loadCancelledRSVPs(page) {
    document.getElementById('refundsLoading').style.display = 'block';
    document.getElementById('refundsContent').style.display = 'none';
    document.getElementById('refundsEmpty').style.display = 'none';

    const url = `/api/events/${eventId}/cancelled-rsvps?page=${page}&size=20`;
    fetch(url)
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                alert(data.error);
                return;
            }

            currentRefundsPage = data.currentPage;
            totalRefundsPages = data.totalPages;

            // Update count badge
            document.getElementById('refundsCount').textContent = data.totalElements || 0;

            if (data.cancelledRsvps.length === 0) {
                document.getElementById('refundsLoading').style.display = 'none';
                document.getElementById('refundsEmpty').style.display = 'block';
                return;
            }

            renderRefundsTable(data.cancelledRsvps);
            renderRefundsPagination();

            document.getElementById('refundsLoading').style.display = 'none';
            document.getElementById('refundsContent').style.display = 'block';
        })
        .catch(error => {
            console.error('Error loading cancelled RSVPs:', error);
            alert('Failed to load cancelled RSVPs');
        });
}

/**
 * Render cancelled RSVPs table
 */
function renderRefundsTable(cancelledRsvps) {
    const tbody = document.getElementById('refundsTableBody');
    tbody.innerHTML = '';

    cancelledRsvps.forEach(cr => {
        const cancelledDate = new Date(cr.cancelledAt).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });

        // Determine type badge color
        let typeBadgeClass = 'bg-info';
        if (cr.displayBadge.includes('Blocked')) {
            typeBadgeClass = 'bg-danger';
        } else if (cr.displayBadge.includes('Organiser')) {
            typeBadgeClass = 'bg-warning text-dark';
        }

        // Determine refund action button/badge
        let refundAction = '';
        if (cr.refundStatus === 'refunded') {
            const refundedDate = new Date(cr.refundedAt).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
            refundAction = `<span class="badge bg-success">Refunded ✓</span><br>
                           <small class="text-muted">${refundedDate}</small>`;
        } else if (cr.refundStatus === 'failed') {
            refundAction = `<span class="badge bg-danger">Refund Failed</span><br>
                           <button class="btn btn-sm btn-warning mt-1"
                                   onclick="refundPayment(${cr.id}, '${cr.username}', ${cr.amountPaid})">
                               <i class="fas fa-redo me-1"></i>Retry
                           </button>`;
        } else if (cr.paymentStatus === 'paid') {
            refundAction = `<button class="btn btn-sm btn-warning"
                                   onclick="refundPayment(${cr.id}, '${cr.username}', ${cr.amountPaid})">
                               <i class="fas fa-undo me-1"></i>Refund Payment
                           </button>`;
        } else {
            refundAction = '<span class="text-muted">No payment</span>';
        }

        const row = `
            <tr>
                <td>${cr.username}</td>
                <td>${cr.email}</td>
                <td>${cancelledDate}</td>
                <td><span class="badge ${typeBadgeClass}">${cr.displayBadge}</span></td>
                <td>$${cr.amountPaid ? cr.amountPaid.toFixed(2) : '0.00'}</td>
                <td>${cr.refundStatus ? cr.refundStatus.charAt(0).toUpperCase() + cr.refundStatus.slice(1) : 'Pending'}</td>
                <td>${refundAction}</td>
            </tr>
        `;
        tbody.innerHTML += row;
    });
}

/**
 * Render pagination for refunds tab
 */
function renderRefundsPagination() {
    const pagination = document.getElementById('refundsPagination').querySelector('ul');
    pagination.innerHTML = '';

    // Previous button
    const prevDisabled = currentRefundsPage === 0 ? 'disabled' : '';
    pagination.innerHTML += `
        <li class="page-item ${prevDisabled}">
            <a class="page-link" href="#" onclick="loadCancelledRSVPs(${currentRefundsPage - 1}); return false;">
                Previous
            </a>
        </li>
    `;

    // Page info
    pagination.innerHTML += `
        <li class="page-item disabled">
            <span class="page-link">
                Page ${currentRefundsPage + 1} of ${totalRefundsPages}
            </span>
        </li>
    `;

    // Next button
    const nextDisabled = currentRefundsPage >= totalRefundsPages - 1 ? 'disabled' : '';
    pagination.innerHTML += `
        <li class="page-item ${nextDisabled}">
            <a class="page-link" href="#" onclick="loadCancelledRSVPs(${currentRefundsPage + 1}); return false;">
                Next
            </a>
        </li>
    `;
}

/**
 * Process refund for cancelled RSVP
 */
function refundPayment(cancelledRsvpId, username, amount) {
    // Show confirmation dialog
    if (!confirm(`Refund $${amount.toFixed(2)} to ${username}?\n\nThis action cannot be undone.`)) {
        return;
    }

    fetch(`/api/events/${eventId}/cancelled-rsvps/${cancelledRsvpId}/refund`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Refund processed successfully!');
                loadCancelledRSVPs(currentRefundsPage); // Reload current page
            } else {
                alert(data.error || 'Failed to process refund');
            }
        })
        .catch(error => {
            console.error('Error processing refund:', error);
            alert('Failed to process refund');
        });
}
```

### 4. JavaScript: Update Modal Setup

**Find `openAttendeesModal()` function** and add refunds tab loading:

```javascript
function openAttendeesModal() {
    const modal = new bootstrap.Modal(document.getElementById('attendeesModal'));
    modal.show();

    // Load all tabs
    loadAttendees(0, '');
    loadBlockedUsers(0);

    // Only load refunds tab if event requires payment
    if (document.getElementById('refunds-tab')) {
        loadCancelledRSVPs(0);
    }

    // Setup tab change event listeners
    document.getElementById('rsvps-tab').addEventListener('click', () => {
        loadAttendees(currentAttendeesPage, currentAttendeesSearch);
    });

    document.getElementById('blocked-tab').addEventListener('click', () => {
        loadBlockedUsers(currentBlockedPage);
    });

    // Only add refunds tab listener if it exists
    const refundsTab = document.getElementById('refunds-tab');
    if (refundsTab) {
        refundsTab.addEventListener('click', () => {
            loadCancelledRSVPs(currentRefundsPage);
        });
    }

    // Setup Enter key for search
    document.getElementById('attendeesSearchInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            searchAttendees();
        }
    });
}
```

---

## Integration Flow

### User Self-Cancellation Flow

1. User clicks "Cancel RSVP" on event details page
2. `RSVPController.cancelRSVP()` is called
3. If paid event:
   - Create `CancelledRSVP` record with `initiated_by = "attendee"`
   - Copy payment info from original RSVP
4. Delete original RSVP from `rsvp` table
5. User sees "RSVP cancelled successfully"
6. Cancelled RSVP appears in organizer's "Refunds" tab

### Organizer Cancellation Flow

1. Organizer clicks "Delete" button in attendees table
2. `EventApiController.cancelAttendeeRsvp()` is called
3. If paid event:
   - Create `CancelledRSVP` record with `initiated_by = "organiser"`
   - Copy payment info from original RSVP
4. Delete original RSVP
5. Attendee removed from "RSVPs" tab
6. Appears in "Refunds" tab with refund option

### Blocking User Flow

1. Organizer clicks "Block" button in attendees table
2. `EventApiController.blockAttendee()` is called
3. If user has RSVP and paid event:
   - Create `CancelledRSVP` record with `initiated_by = "organiser"`
   - Copy payment info
4. Delete RSVP
5. Create `BlockedRSVP` record
6. User appears in BOTH:
   - "Blocked Users" tab
   - "Refunds" tab (if paid event)

### Refund Processing Flow

1. Organizer opens "Refunds" tab in attendees modal
2. Sees list of cancelled/blocked RSVPs with payment info
3. Clicks "Refund Payment" button for specific user
4. Confirmation dialog appears: "Refund $X.XX to username?"
5. On confirm:
   - Frontend calls `/api/events/{id}/cancelled-rsvps/{id}/refund`
   - Backend validates organizer permission
   - Calls Stripe API `Refund.create()`
   - Updates `CancelledRSVP` record:
     - `refund_status = "refunded"`
     - `refunded_at = now()`
     - `stripe_refund_id = refund.getId()`
6. Button changes to green "Refunded ✓" badge
7. Row shows refund timestamp

### Re-RSVP After Cancellation Flow

1. User previously cancelled RSVP (record in `cancelled_rsvps`)
2. User clicks "RSVP Now" again
3. New RSVP created in `rsvp` table
4. User pays again (new payment)
5. Old cancelled record stays in `cancelled_rsvps` (historical)
6. New active RSVP shows in "RSVPs" tab
7. Old cancelled RSVP stays in "Refunds" tab (audit trail)

---

## Tab Visibility Rules

### Free Events (price < $0.05 or null)
- ✅ "RSVPs" tab - always visible
- ✅ "Blocked Users" tab - always visible
- ❌ "Refunds" tab - **HIDDEN** (no payments to track)

### Paid Events (price >= $0.05)
- ✅ "RSVPs" tab - always visible
- ✅ "Blocked Users" tab - always visible
- ✅ "Refunds (Cancelled/Blocked)" tab - **VISIBLE**

---

## Testing Checklist

### Database
- [ ] `cancelled_rsvps` table created with all fields
- [ ] Foreign key constraints working
- [ ] Indexes created for performance

### Backend
- [ ] User cancels own RSVP → creates cancelled record with `initiated_by="attendee"`
- [ ] Organizer cancels attendee → creates cancelled record with `initiated_by="organiser"`
- [ ] Blocking user → creates cancelled record (if paid) + blocked record
- [ ] Refund API processes payment via Stripe
- [ ] Refund updates `refund_status` to "refunded"
- [ ] Failed refund sets `refund_status` to "failed"
- [ ] Only organizer/admin can view cancelled RSVPs
- [ ] Only organizer/admin can process refunds

### Frontend
- [ ] "Refunds" tab only shows for paid events
- [ ] Tab loads cancelled RSVPs correctly
- [ ] Type badges show correct colors (Blocked/Cancelled-Self/Cancelled-Organiser)
- [ ] Refund button shows for paid, unrefunded cancellations
- [ ] "Refunded ✓" badge shows after successful refund
- [ ] Confirmation dialog appears before refund
- [ ] Failed refunds show "Retry" button
- [ ] Pagination works correctly
- [ ] Tab badge shows total count

### Edge Cases
- [ ] User cancels free event RSVP → no cancelled record created
- [ ] User re-RSVPs after cancellation → both records exist independently
- [ ] Organizer blocks user with no RSVP → only blocked record created
- [ ] Refund same payment twice → second attempt fails with error
- [ ] Delete event → cascades to cancelled_rsvps table

---

## Stripe Test Mode Notes

- All refunds work in Stripe test mode exactly like production
- Use test payment intents from DataInitializer for testing
- Test card: 4242 4242 4242 4242
- Refund appears instantly in Stripe Dashboard (test mode)
- No actual money is transferred
- Webhook for `charge.refunded` can be implemented later for real-time updates

---

## Future Enhancements (Out of Scope for MVP)

1. **Partial refunds** - Allow organizer to refund less than full amount
2. **Refund reasons** - Add dropdown for cancellation reason
3. **Bulk refunds** - Select multiple cancelled RSVPs and refund all at once
4. **Email notifications** - Auto-email user when refund is processed
5. **Refund analytics** - Dashboard showing refund statistics
6. **Webhook for refunds** - Listen to `charge.refunded` event from Stripe
7. **Export cancelled RSVPs** - CSV export with refund history

---

## Implementation Order

1. **Database** - Create `cancelled_rsvps` table
2. **Models** - Create `CancelledRSVP` entity and DTO
3. **Repository** - Create `CancelledRSVPRepository`
4. **Services** - Update `RSVPService`, add refund methods to `EventService` and `StripeService`
5. **Controllers** - Add API endpoints to `EventApiController`
6. **Frontend** - Add tab HTML, JavaScript functions, and integrate with modal

---

**Last Updated**: 2025-10-05
**Status**: Design Complete - Ready for Implementation
**Estimated Effort**: 6-8 hours

---------------------------

⏺ Perfect! Updated openAttendeesModal() with:

  ✅ Loads refunds tab data on modal open (only if tab exists - paid events)✅ Added event listener for refunds tab click✅
   Checks for tab existence with document.getElementById('refunds-tab') before attaching listeners

  Refunds Feature Complete! 🎉

  All implementation layers finished:

  ✅ Database

  - cancelled_rsvps table with all fields
  - Indexes for performance

  ✅ Backend

  - CancelledRSVP model
  - CancelledRSVPRepository with query methods
  - CancelledRSVPDTO
  - StripeService.refundPayment()
  - RSVPService - creates cancelled records on cancel/block
  - EventService - getCancelledRSVPs() and refundCancelledRSVP()
  - EventApiController - API endpoints

  ✅ Frontend

  - Tab navigation (paid events only)
  - Tab content pane with table
  - JavaScript functions (load, render, paginate, refund)
  - Modal integration

  The feature is ready to test!