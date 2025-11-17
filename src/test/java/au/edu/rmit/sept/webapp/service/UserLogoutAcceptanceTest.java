package au.edu.rmit.sept.webapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserLogoutAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user for logout testing
        testUser = new User();
        testUser.setUsername("logout.user");
        testUser.setEmail("logout@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEnabled(true);
        userRepository.save(testUser);
    }

    @Test
    void testSuccessfulLogout_CompleteFlow() throws Exception {
        // Step 1: Verify user can access protected endpoint while authenticated
        mockMvc.perform(get("/events/create")
                .with(user(testUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"));

        // Step 2: POST to logout endpoint
        mockMvc.perform(post("/logout")
                .with(user(testUser.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));

        // Step 3: Verify logout success message is displayed
        mockMvc.perform(get("/login?logout=true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("logoutMessage", "You have been successfully logged out"));
    }

    @Test
    void testSessionInvalidation_AfterLogout() throws Exception {
        // Step 1: Logout user
        mockMvc.perform(post("/logout")
                .with(user(testUser.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));

        // Step 2: Verify user cannot access protected endpoints after logout
        mockMvc.perform(get("/events/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testLogout_WhenNotAuthenticated() throws Exception {
        // Unauthenticated user should still be able to logout gracefully
        mockMvc.perform(post("/logout")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    @Test
    void testLogoutWithRememberMe_ClearsSession() throws Exception {
        // Step 1: Login with remember-me
        mockMvc.perform(post("/login")
                .param("username", "logout.user")
                .param("password", "password123")
                .param("remember-me", "true")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        // Step 2: Logout (should clear remember-me cookie)
        mockMvc.perform(post("/logout")
                .with(user(testUser.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));

        // Step 3: Verify user needs to login again (remember-me cleared)
        mockMvc.perform(get("/events/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
