package au.edu.rmit.sept.webapp.service;
// mvn test -Dtest=*AcceptanceTest
// mvn test -Dtest=UserRegistrationAcceptanceTest
// mvn test -Dtest=*ServiceTest

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserRegistrationAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Category techCategory;
    private Category sportsCategory;

    @BeforeEach
    void setUp() {
        // Just fetch existing categories
        techCategory = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Technology"))
                .findFirst()
                .orElseThrow();

        sportsCategory = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Sports"))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void testSuccessfulUserRegistration_CompleteFlow() throws Exception {
        // Step 1: GET registration page
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("userRegistrationDTO"))
                .andExpect(model().attributeExists("categories"));

        // Step 2: POST valid registration form
        mockMvc.perform(post("/register")
                .param("username", "test.newuser")
                .param("email", "test.newuser@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("categoryIds", techCategory.getId().toString(), sportsCategory.getId().toString())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?success=true"));

        // Step 3: Verify user created in database
        User savedUser = userRepository.findByUsername("test.newuser").orElseThrow();
        assertEquals("test.newuser@example.com", savedUser.getEmail());
        assertTrue(passwordEncoder.matches("password123", savedUser.getPassword()));
        assertEquals(2, savedUser.getCategories().size());
        assertTrue(savedUser.isEnabled());

        // Step 4: Verify user can login with new credentials
        mockMvc.perform(post("/login")
                .param("username", "test.newuser")
                .param("password", "password123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void testRegistrationValidationErrors_StaysOnForm() throws Exception {
        long userCountBeforeTest = userRepository.count();
        
        // Test multiple validation failures with realistic data that could bypass client-side validation
        mockMvc.perform(post("/register")
                .param("username", "") // Blank username
                .param("email", "invalid-email") // Invalid email format
                .param("password", "password123") // Valid password
                .param("confirmPassword", "differentpass123") // Doesn't match - realistic scenario
                .param("categoryIds", "") // No categories
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("userRegistrationDTO",
                        "username", "email", "categoryIds"))
                .andExpect(model().attributeExists("categories")); // Categories reloaded for form

        // Verify no new user created
        assertEquals(userCountBeforeTest, userRepository.count());
    }

    @Test
    void testDuplicateUsername_ShowsError() throws Exception {
        // Create existing user with unique username
        User existing = new User("test.duplicate", "existing@example.com", passwordEncoder.encode("password"));
        userRepository.save(existing);
        long initialUserCount = userRepository.count();

        // Attempt registration with same username
        mockMvc.perform(post("/register")
                .param("username", "test.duplicate")
                .param("email", "new@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("categoryIds", techCategory.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrorCode("userRegistrationDTO", "username", "error.username"));

        // Verify no new user created
        assertEquals(initialUserCount, userRepository.count());
    }

    @Test
    void testAuthenticatedUserCannotAccessRegistration() throws Exception {
        // Login first
        User user = userRepository.save(new User("existing", "test@test.com", passwordEncoder.encode("password")));

        mockMvc.perform(get("/register")
                .with(user(user.getUsername())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }
}