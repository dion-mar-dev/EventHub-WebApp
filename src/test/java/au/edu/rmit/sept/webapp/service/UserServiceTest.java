package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.dto.UserRegistrationDTO;
import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataAccessException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationDTO validDTO;
    private Category techCategory;
    private Category sportsCategory;

    @BeforeEach
    void setUp() {
        // Create valid registration DTO
        validDTO = new UserRegistrationDTO();
        validDTO.setUsername("john.doe");
        validDTO.setEmail("john@example.com");
        validDTO.setPassword("password123");
        validDTO.setConfirmPassword("password123");
        validDTO.setCategoryIds(Arrays.asList(1L, 2L));

        // Create test categories
        techCategory = new Category("Technology", "Tech events", "#5dade2");
        techCategory.setId(1L);

        sportsCategory = new Category("Sports", "Sports events", "#ff8c69");
        sportsCategory.setId(2L);
    }

    @Test
    void register_Success_WithValidCategories() {
        // Arrange
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(sportsCategory));

        User savedUser = new User();
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.register(validDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());

        // Verify saved user details using ArgumentCaptor
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        // ArgumentCaptor intercepts and stores the User object at the exact moment it
        // is passed as an argument to the mocked userRepository.save() method after the
        // UserService has converted the DTO to a user entity. The captor allows the
        // test to inspect this final, prepared object before the mock repository's
        // thenReturn() behavior is executed.
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertEquals("john.doe", capturedUser.getUsername());
        assertEquals("john@example.com", capturedUser.getEmail());
        assertEquals("hashedPassword123", capturedUser.getPassword());
        assertEquals(2, capturedUser.getCategories().size());
        assertTrue(capturedUser.getCategories().contains(techCategory));
        assertTrue(capturedUser.getCategories().contains(sportsCategory));

        verify(passwordEncoder).encode("password123");
    }
    // The DTO validation @Size(min = 1, message = "Please select at least one
    // category") prevents null/empty categories from ever reaching the
    // UserService.register() method. Spring's validation framework would reject the
    // request before it gets to the service layer.
    // These tests are testing unreachable code paths - the service method will
    // never receive a DTO with null or empty categories in a real application flow.
    @Test
    void register_Success_WithNullCategories() {
        // Arrange
        validDTO.setCategoryIds(null);
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");

        User savedUser = new User();
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.register(validDTO);

        // Assert
        assertNotNull(result);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertTrue(capturedUser.getCategories().isEmpty());
    }
    
    // These tests are testing unreachable code paths - the service method will
    // never receive a DTO with null or empty categories in a real application flow.
    @Test
    void register_Success_WithEmptyCategories() {
        // Arrange
        validDTO.setCategoryIds(Arrays.asList());
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");

        User savedUser = new User();
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.register(validDTO);

        // Assert
        assertNotNull(result);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().getCategories().isEmpty());
    }

    @Test
    void register_ThrowsException_WhenPasswordsDoNotMatch() {
        // Arrange
        validDTO.setConfirmPassword("differentPassword");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(validDTO));

        assertEquals("Passwords do not match", exception.getMessage());

        // Verify no database interactions
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ThrowsException_WhenUsernameAlreadyTaken() {
        // Arrange
        when(userRepository.existsByUsername("john.doe")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(validDTO));

        assertEquals("Username already taken", exception.getMessage());

        // Verify password check occurred first
        verify(userRepository).existsByUsername("john.doe");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ThrowsException_WhenEmailAlreadyRegistered() {
        // Arrange
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(validDTO));

        assertEquals("Email already registered", exception.getMessage());

        // Verify checks occurred in order
        verify(userRepository).existsByUsername("john.doe");
        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ThrowsException_WhenInvalidCategoryId() {
        // Arrange
        validDTO.setCategoryIds(Arrays.asList(1L, 999L));
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(validDTO));

        assertEquals("Invalid category ID: 999", exception.getMessage());

        // Verify no user was saved
        verify(userRepository, never()).save(any());
    }

    @Test
    void existsByUsername_ReturnsTrue_WhenUsernameExists() {
        // Arrange
        when(userRepository.existsByUsername("existing.user")).thenReturn(true);

        // Act
        boolean exists = userService.existsByUsername("existing.user");

        // Assert
        assertTrue(exists);
        verify(userRepository).existsByUsername("existing.user");
    }

    @Test
    void existsByUsername_ReturnsFalse_WhenUsernameDoesNotExist() {
        // Arrange
        when(userRepository.existsByUsername("new.user")).thenReturn(false);

        // Act
        boolean exists = userService.existsByUsername("new.user");

        // Assert
        assertFalse(exists);
        verify(userRepository).existsByUsername("new.user");
    }

    @Test
    void existsByEmail_ReturnsTrue_WhenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act
        boolean exists = userService.existsByEmail("existing@example.com");

        // Assert
        assertTrue(exists);
        verify(userRepository).existsByEmail("existing@example.com");
    }

    @Test
    void existsByEmail_ReturnsFalse_WhenEmailDoesNotExist() {
        // Arrange
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        // Act
        boolean exists = userService.existsByEmail("new@example.com");

        // Assert
        assertFalse(exists);
        verify(userRepository).existsByEmail("new@example.com");
    }

    @Test
    void getUserIdByUsername_ReturnsId_WhenUserExists() {
        // Arrange
        User user = new User();
        user.setId(123L);
        user.setUsername("john.doe");
        when(userRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));

        // Act
        Long userId = userService.getUserIdByUsername("john.doe");

        // Assert
        assertEquals(123L, userId);
        verify(userRepository).findByUsername("john.doe");
    }

    @Test
    void getUserIdByUsername_ReturnsNull_WhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        Long userId = userService.getUserIdByUsername("nonexistent");

        // Assert
        assertNull(userId);
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void getUsernameById_ReturnsUsername_WhenUserExists() {
        // Arrange
        User user = new User();
        user.setId(123L);
        user.setUsername("john.doe");
        when(userRepository.findById(123L)).thenReturn(Optional.of(user));

        // Act
        String username = userService.getUsernameById(123L);

        // Assert
        assertEquals("john.doe", username);
        verify(userRepository).findById(123L);
    }

    @Test
    void getUsernameById_ReturnsNull_WhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        String username = userService.getUsernameById(999L);

        // Assert
        assertNull(username);
        verify(userRepository).findById(999L);
    }

    @Test
    void register_ChecksValidationInCorrectOrder() {
        // Arrange - Set up all checks to fail
        validDTO.setConfirmPassword("wrongPassword");

        // Act & Assert - Should fail on password mismatch first
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(validDTO));

        assertEquals("Passwords do not match", exception.getMessage());

        // Verify no database checks occurred
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void register_ThrowsException_WhenRepositorySaveFails() {
        // Arrange - everything valid but save fails
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(sportsCategory));
        when(userRepository.save(any())).thenThrow(new DataAccessException("DB error") {});

        // Act & Assert
        assertThrows(DataAccessException.class, () -> userService.register(validDTO));
    }

    // ============== hasRole() ==============

    @Test
    void hasRole_ReturnsTrue_WhenUserHasRole() {
        User user = new User();
        user.setId(1L);
        user.setRole("ROLE_ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        boolean result = userService.hasRole(1L, "ROLE_ADMIN");

        assertTrue(result);
    }

    @Test
    void hasRole_ReturnsFalse_WhenUserDoesNotHaveRole() {
        User user = new User();
        user.setId(1L);
        user.setRole("ROLE_USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        boolean result = userService.hasRole(1L, "ROLE_ADMIN");

        assertFalse(result);
    }

    @Test
    void hasRole_ReturnsFalse_WhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = userService.hasRole(999L, "ROLE_ADMIN");

        assertFalse(result);
    }

    // ============== findByUsername() ==============

    @Test
    void findByUsername_ReturnsUser_WhenUserExists() {
        User user = new User();
        user.setId(1L);
        user.setUsername("john.doe");

        when(userRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername("john.doe");

        assertTrue(result.isPresent());
        assertEquals("john.doe", result.get().getUsername());
    }

    @Test
    void findByUsername_ReturnsEmpty_WhenUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername("nonexistent");

        assertFalse(result.isPresent());
    }

    // ============== register() - Additional Edge Cases ==============

    @Test
    void register_VerifiesPasswordIsEncoded() {
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPassword");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(sportsCategory));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(validDTO);

        verify(passwordEncoder).encode("password123");
        assertNotEquals("password123", result.getPassword());
        assertEquals("$2a$10$hashedPassword", result.getPassword());
    }

    @Test
    void register_AssociatesMultipleCategories() {
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(sportsCategory));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(validDTO);

        assertNotNull(result.getCategories());
        assertEquals(2, result.getCategories().size());
        assertTrue(result.getCategories().contains(techCategory));
        assertTrue(result.getCategories().contains(sportsCategory));
    }
}
