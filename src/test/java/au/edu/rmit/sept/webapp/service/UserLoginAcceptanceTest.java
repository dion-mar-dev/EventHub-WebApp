package au.edu.rmit.sept.webapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import jakarta.servlet.http.Cookie;

import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.UserRepository;

// mvn test -Dtest=UserLoginAcceptanceTest

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserLoginAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        // Create test user for login
        testUser = new User();
        testUser.setUsername("john.doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEnabled(true);
        userRepository.save(testUser);

        // Create disabled user for edge case testing
        disabledUser = new User();
        disabledUser.setUsername("disabled.user");
        disabledUser.setEmail("disabled@example.com");
        disabledUser.setPassword(passwordEncoder.encode("password123"));
        disabledUser.setEnabled(false);
        userRepository.save(disabledUser);
    }

    @Test
    void testSuccessfulLogin_WithUsername_CompleteFlow() throws Exception {
        // Step 1: GET login page
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));

        // Step 2: POST valid credentials with username
        mockMvc.perform(post("/login")
                .param("username", "john.doe")
                .param("password", "password123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        // Step 3: Verify user can access protected endpoints after login
        // Login first by authenticating the user
        mockMvc.perform(get("/events/create")
                .with(user(testUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"));
    }

    @Test
    void testSuccessfulLogin_WithEmail() throws Exception {
        // Login with email instead of username (CustomUserDetailsService supports both)
        mockMvc.perform(post("/login")
                .param("username", "john.doe@example.com") // Using email in username field
                .param("password", "password123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void testFailedLogin_InvalidCredentials() throws Exception {
        // Test wrong password
        mockMvc.perform(post("/login")
                .param("username", "john.doe")
                .param("password", "wrongpassword")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));

        // Verify error message is displayed when accessing login page after failure
        mockMvc.perform(get("/login?error=true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("errorMessage", "Invalid username or password"));
    }

    @Test
    void testFailedLogin_NonexistentUser() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "nonexistent.user")
                .param("password", "password123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void testFailedLogin_EmptyCredentials() throws Exception {
        // HTML5 required attributes should prevent this, but test server-side handling
        mockMvc.perform(post("/login")
                .param("username", "")
                .param("password", "")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void testFailedLogin_DisabledAccount() throws Exception {
        // Test login with disabled account
        mockMvc.perform(post("/login")
                .param("username", "disabled.user")
                .param("password", "password123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void testLogin_CaseSensitivity() throws Exception {
        // Username should be case-sensitive (typical behavior)
        mockMvc.perform(post("/login")
                .param("username", "John.Doe") // Different case
                .param("password", "password123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));

        // Email should also be case-sensitive for security
        mockMvc.perform(post("/login")
                .param("username", "John.Doe@Example.com") // Different case
                .param("password", "password123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void testAuthenticatedUserCannotAccessLogin() throws Exception {
        // Already authenticated user should be redirected to home
        mockMvc.perform(get("/login")
                .with(user(testUser.getUsername())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void testUnauthenticatedUserCannotAccessProtectedEndpoints() throws Exception {
        // Test that protected endpoints redirect to login
        mockMvc.perform(get("/events/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Test accessing RSVP functionality without authentication
        mockMvc.perform(post("/rsvp")
                .param("eventId", "1")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void testAccessProtectedEndpointsAfterLogin() throws Exception {
        // Verify authenticated user can access protected endpoints
        mockMvc.perform(get("/events/create")
                .with(user(testUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"));

        // Verify user info is available in authenticated context
        mockMvc.perform(get("/")
                .with(user(testUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("username", "john.doe"));
    }

    @Test
    void testLogoutSuccessMessage() throws Exception {
        // Test logout success parameter displays correct message
        mockMvc.perform(get("/login?logout=true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("logoutMessage", "You have been successfully logged out"));
    }

    @Test
    void testSessionExpiredMessage() throws Exception {
        // Test session expired parameter displays correct message
        mockMvc.perform(get("/login?expired=true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("expiredMessage", "Session expired. Please login again"));
    }

    @Test
    void testRememberMeLogin_SetsCookie() throws Exception {
        // Test that remember me login sets the remember-me cookie
        mockMvc.perform(post("/login")
                .param("username", "john.doe")
                .param("password", "password123")
                .param("remember-me", "on") // Enable remember me
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(cookie().exists("remember-me"))
                .andExpect(cookie().maxAge("remember-me", 1209600)); // 14 days in seconds
    }

    @Test
    void testRememberMeLogin_WithoutCheckbox() throws Exception {
        // Test that login without remember-me checkbox doesn't set cookie
        mockMvc.perform(post("/login")
                .param("username", "john.doe")
                .param("password", "password123")
                // No remember-me parameter
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(cookie().doesNotExist("remember-me"));
    }

    @Test
    void testRememberMeLogin_PersistentAccess() throws Exception {
        // Step 1: Login with remember me enabled
        Cookie rememberMeCookie = mockMvc.perform(post("/login")
                .param("username", "john.doe")
                .param("password", "password123")
                .param("remember-me", "on")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(cookie().exists("remember-me"))
                .andReturn()
                .getResponse()
                .getCookie("remember-me");

        // Step 2: Access protected endpoint with only remember-me cookie (no session)
        mockMvc.perform(get("/events/create")
                .cookie(rememberMeCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"));
    }

    @Test 
    void testRememberMeLogin_InvalidCookie() throws Exception {
        // Test that invalid remember-me cookie doesn't grant access
        Cookie invalidCookie = new Cookie("remember-me", "invalid-token-value");
        
        mockMvc.perform(get("/events/create")
                .cookie(invalidCookie))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testLogout_DeletesRememberMeCookie() throws Exception {
        // Step 1: Login with remember me
        Cookie rememberMeCookie = mockMvc.perform(post("/login")
                .param("username", "john.doe")
                .param("password", "password123")
                .param("remember-me", "on")
                .with(csrf()))
                .andExpect(cookie().exists("remember-me"))
                .andReturn()
                .getResponse()
                .getCookie("remember-me");

        // Step 2: Logout should delete the remember-me cookie
        mockMvc.perform(post("/logout")
                .cookie(rememberMeCookie)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"))
                .andExpect(cookie().maxAge("remember-me", 0)); // Cookie deleted (maxAge=0)
    }
}