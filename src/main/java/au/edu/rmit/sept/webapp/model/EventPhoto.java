package au.edu.rmit.sept.webapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * EventPhoto Entity
 * Represents photo metadata for event galleries.
 *
 * BUSINESS RULES:
 * - Up to 20 photos per event
 * - Only organizers/admins can upload/delete
 * - Only RSVP'd attendees can view
 * - Photos can only be added after event has passed
 *
 * STORAGE:
 * - dev/devprod: Local filesystem (uploads/events/{eventId}/)
 * - prod: Google Cloud Storage
 * - filename is UUID-based (e.g., abc123-def456.jpg)
 * - originalFilename is user's upload name (for display)
 */
@Entity
@Table(name = "event_photos")
public class EventPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // File metadata
    @NotBlank(message = "Filename cannot be blank")
    @Size(max = 255, message = "Filename cannot exceed 255 characters")
    @Column(nullable = false)
    private String filename;

    @NotBlank(message = "Original filename cannot be blank")
    @Size(max = 255, message = "Original filename cannot exceed 255 characters")
    @Column(nullable = false)
    private String originalFilename;

    @NotNull(message = "File size cannot be null")
    @Column(nullable = false)
    private Long fileSize;

    // Upload metadata
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @NotNull(message = "Event cannot be null")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    @NotNull(message = "Uploader cannot be null")
    private User uploadedBy;

    // Constructors
    public EventPhoto() {
    }

    public EventPhoto(String filename, String originalFilename, Long fileSize, Event event, User uploadedBy) {
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.event = event;
        this.uploadedBy = uploadedBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}
