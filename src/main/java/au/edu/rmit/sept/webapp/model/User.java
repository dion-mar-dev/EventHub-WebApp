package au.edu.rmit.sept.webapp.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;

    // @Column(name = "account_expired")
    // private boolean accountExpired = false;

    // @Column(name = "account_locked") 
    // private boolean accountLocked = false;

    // @Column(name = "credentials_expired")
    // private boolean credentialsExpired = false;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "deactivated")
    private boolean deactivated = false;

    @Column(name = "deactivated_by_admin_id")
    private Long deactivatedByAdminId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "role")
    private String role = "ROLE_USER";
    
    @ManyToMany
    @JoinTable(
        name = "user_categories",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();
    
    // Default constructor
    public User() {}
    
    // Constructor with parameters
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Set<Category> getCategories() {
        return categories;
    }
    
    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getRole() { return role;}
    public void setRole(String role) { this.role = role;}

    public boolean isDeactivated() { return deactivated; }
    public void setDeactivated(boolean deactivated) { this.deactivated = deactivated; }

    public Long getDeactivatedByAdminId() { return deactivatedByAdminId; }
    public void setDeactivatedByAdminId(Long deactivatedByAdminId) { this.deactivatedByAdminId = deactivatedByAdminId; }
}