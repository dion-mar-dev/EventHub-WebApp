package au.edu.rmit.sept.webapp.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "colour_code")
    private String colourCode;
    
    @ManyToMany(mappedBy = "categories")
    private Set<User> users = new HashSet<>();
    
    // Default constructor
    public Category() {}
    
    // Constructor with parameters
    public Category(String name, String description, String colourCode) {
        this.name = name;
        this.description = description;
        this.colourCode = colourCode;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getColourCode() {
        return colourCode;
    }
    
    public void setColourCode(String colourCode) {
        this.colourCode = colourCode;
    }
    
    public Set<User> getUsers() {
        return users;
    }
    
    public void setUsers(Set<User> users) {
        this.users = users;
    }
}