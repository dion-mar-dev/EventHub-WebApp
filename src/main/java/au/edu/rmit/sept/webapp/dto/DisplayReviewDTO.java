package au.edu.rmit.sept.webapp.dto;
import java.time.LocalDateTime;

public class DisplayReviewDTO {

    //Output DTO only needs rating, comment and username
    private Integer rating;
    private String comment;
    private String authorUsername;
    private LocalDateTime createdAt;

    // Getters and Setters
    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}