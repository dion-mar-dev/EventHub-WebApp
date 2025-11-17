package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.dto.UserRegistrationDTO;
import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection - modern Spring best practice for immutability, testability, and fail-fast behavior
    public UserService(UserRepository userRepository, CategoryRepository categoryRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Transactional
    public User register(UserRegistrationDTO registrationDTO) {
        // Check if passwords match
        if (!registrationDTO.isPasswordMatching()) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        
        // Check for duplicate username
        if (userRepository.existsByUsername(registrationDTO.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        
        // Check for duplicate email
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        // Create new user
        User user = new User();
        user.setUsername(registrationDTO.getUsername());
        user.setEmail(registrationDTO.getEmail());
        
        // Hash password with BCrypt
        String hashedPassword = passwordEncoder.encode(registrationDTO.getPassword());
        user.setPassword(hashedPassword);
        
        // Associate selected categories
        Set<Category> categories = new HashSet<>();
        List<Long> categoryIds = registrationDTO.getCategoryIds();
        
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Long categoryId : categoryIds) {
                Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid category ID: " + categoryId));
                categories.add(category);
            }
        }
        
        user.setCategories(categories);
        
        // Save user to database
        return userRepository.save(user);
    }
    
    // Helper method to check if username exists
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    // Helper method to check if email exists
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    // Get user ID by username for authentication context
    public Long getUserIdByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.map(User::getId).orElse(null);
    }

    // Get username by user ID for RSVP status checking
    public String getUsernameById(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(User::getUsername).orElse(null);
    }

    // Get user by username
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Checks if a user has a specific role.
     * @param userId The ID of the user to check
     * @param role The role to check for (e.g., "ROLE_ADMIN")
     * @return true if the user has the role, false otherwise
     */
    public boolean hasRole(Long userId, String role) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(u -> role.equals(u.getRole())).orElse(false);
    }
}