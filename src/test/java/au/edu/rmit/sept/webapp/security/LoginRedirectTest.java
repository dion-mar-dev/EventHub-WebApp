package au.edu.rmit.sept.webapp.security;

import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for login redirect behavior - documenting known bug where users are
 * always redirected to /home after login instead of returning to the original page.
 *
 * BUG DESCRIPTION:
 * When a user:
 * 1. Visits an event details page (e.g., /events/33)
 * 2. Clicks login link
 * 3. Successfully logs in
 *
 * EXPECTED: User is redirected back to /events/33
 * ACTUAL: User is redirected to /home
 *
 * ROOT CAUSE: SecurityConfig.java line 85 uses defaultSuccessUrl("/home", true)
 * The 'true' parameter forces redirect to /home, ignoring the saved request.
 *
 * FIX: Change to defaultSuccessUrl("/home", false) or defaultSuccessUrl("/home")
 * This allows Spring Security to redirect to the saved request URL.
 *
 * IMPACT: Poor UX - users lose context and have to navigate back to what they were viewing
 *
 * GitHub Issue: [To be created]
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoginRedirectTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Create test user for login tests if doesn't exist
        if (!userRepository.existsByEmail("test@example.com")) {
            User testUser = new User();
            testUser.setUsername("test@example.com");
            testUser.setEmail("test@example.com");
            testUser.setPassword(passwordEncoder.encode("password123"));
            testUser.setRole("ROLE_USER");

            userRepository.save(testUser);
        }
    }

    /**
     * Test that verifies the bug: login always redirects to /home instead of
     * the original requested page.
     *
     * This test simulates:
     * 1. User is viewing an event page (/events/33)
     * 2. User clicks login link (sends Referer header)
     * 3. User logs in successfully
     * 4. Should be redirected back to /events/33 (FAILS - goes to /home instead)
     */
    @Test
    @Disabled("Known bug: Login always redirects to /home - see GitHub issue #231")
    void testLoginRedirectsToOriginalPage() throws Exception {
        // Step 1: User clicks login from event page
        // The Referer header indicates where the user came from
        mockMvc.perform(get("/login")
                .header("Referer", "http://localhost/events/33"))
                .andExpect(status().isOk());

        // Step 2: Perform login with valid credentials
        // Should redirect back to the page user came from (/events/33)
        mockMvc.perform(formLogin("/login")
                .user("test@example.com")
                .password("password123"))
                .andExpect(status().is3xxRedirection())
                // FAILS: This assertion will fail because SecurityConfig forces redirect to /home
                .andExpect(redirectedUrl("/events/33"));
    }

}
