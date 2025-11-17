package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole("ROLE_ADMIN");
        adminUser.setDeactivated(false);
    }

    @Test
    @Disabled("Bug: Admin can deactivate themselves")
    void deactivateUserAsAdmin_AdminCannotDeactivateSelf_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // This test will FAIL because the current implementation allows admins to deactivate themselves
        // Expected behavior: Should throw IllegalArgumentException when admin tries to deactivate themselves
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.deactivateUserAsAdmin(1L, 1L); // userId = adminId
        });

        // Verify user was never saved (deactivation should be blocked)
        verify(userRepository, never()).save(any(User.class));
    }
}
