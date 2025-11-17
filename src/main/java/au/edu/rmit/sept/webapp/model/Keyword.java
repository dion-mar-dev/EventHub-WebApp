package au.edu.rmit.sept.webapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "keywords")
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Keyword name is required")
    @Size(max = 16, message = "Keyword name cannot exceed 16 characters")
    @Column(unique = true, nullable = false)
    private String name;

    @Column(length = 7) // For hex color codes like #FFFFFF
    private String color;

    @ManyToMany(mappedBy = "keywords")
    private Set<Event> events = new HashSet<>();

    // Constructors
    public Keyword() {
    }

    public Keyword(String name, String color) {
        this.name = name;
        this.color = color;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Set<Event> getEvents() {
        return events;
    }

    public void setEvents(Set<Event> events) {
        this.events = events;
    }
}