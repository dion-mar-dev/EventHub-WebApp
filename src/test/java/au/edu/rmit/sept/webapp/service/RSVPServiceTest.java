package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.BlockedRSVPRepository;
import au.edu.rmit.sept.webapp.repository.CancelledRSVPRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.PaymentRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.RSVPService;
import au.edu.rmit.sept.webapp.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RSVPServiceTest {

    @Mock
    private RSVPRepository rsvpRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlockedRSVPRepository blockedRSVPRepository;

    @Mock
    private CancelledRSVPRepository cancelledRSVPRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private RSVPService rsvpService;

    private User testUser;
    private Event testEvent;
    private User organizer;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john.doe");
        testUser.setEmail("john@example.com");

        // Create organizer
        organizer = new User();
        organizer.setId(2L);
        organizer.setUsername("organizer");
        organizer.setEmail("organizer@example.com");

        // Create test event (future date)
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setTitle("Spring Boot Workshop");
        testEvent.setEventDate(LocalDate.now().plusDays(7));
        testEvent.setEventTime(LocalTime.of(14, 0));
        testEvent.setCapacity(30);
        testEvent.setCreatedBy(organizer);
    }

    @Test
    void createRSVP_Success_WhenEventHasCapacity() {
        // Arrange
        when(rsvpRepository.existsByUserAndEvent(testUser, testEvent)).thenReturn(false);
        when(rsvpRepository.countByEvent(testEvent)).thenReturn(25L); // Under capacity

        // Act
        assertDoesNotThrow(() -> rsvpService.createRSVP(testUser, testEvent));

        // Assert
        verify(rsvpRepository, times(1)).save(any(RSVP.class));
    }

    @Test
    void createRSVP_ThrowsException_WhenAlreadyRSVPd() {
        // Arrange
        when(rsvpRepository.existsByUserAndEvent(testUser, testEvent)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> rsvpService.createRSVP(testUser, testEvent));

        assertEquals("You have already RSVP'd to this event", exception.getMessage());
        verify(rsvpRepository, never()).save(any(RSVP.class));
    }

    @Test
    void createRSVP_ThrowsException_WhenEventFull() {
        // Arrange
        when(rsvpRepository.existsByUserAndEvent(testUser, testEvent)).thenReturn(false);
        when(rsvpRepository.countByEvent(testEvent)).thenReturn(30L); // At capacity

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> rsvpService.createRSVP(testUser, testEvent));

        assertEquals("This event is full", exception.getMessage());
        verify(rsvpRepository, never()).save(any(RSVP.class));
    }

    @Test
    void createRSVP_Success_WhenEventHasUnlimitedCapacity() {
        // Arrange
        testEvent.setCapacity(null); // Unlimited capacity
        when(rsvpRepository.existsByUserAndEvent(testUser, testEvent)).thenReturn(false);

        // Act
        assertDoesNotThrow(() -> rsvpService.createRSVP(testUser, testEvent));

        // Assert - no need to check capacity
        verify(rsvpRepository, times(1)).save(any(RSVP.class));
        verify(rsvpRepository, never()).countByEvent(any());
    }

    @Test
    void cancelRSVP_Success_WhenRSVPExists() {
        // Arrange
        RSVP existingRSVP = new RSVP(testUser, testEvent);
        when(rsvpRepository.findByUser_UsernameAndEvent_Id("john.doe", 1L))
                .thenReturn(Optional.of(existingRSVP));

        // Act
        assertDoesNotThrow(() -> rsvpService.cancelRSVP(1L, "john.doe"));

        // Assert
        verify(rsvpRepository, times(1)).delete(existingRSVP);
    }

    @Test
    void cancelRSVP_ThrowsException_WhenRSVPNotFound() {
        // Arrange
        when(rsvpRepository.findByUser_UsernameAndEvent_Id("john.doe", 1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> rsvpService.cancelRSVP(1L, "john.doe"));

        assertEquals("RSVP not found", exception.getMessage());
        verify(rsvpRepository, never()).delete(any());
    }

    @Test
    void getAttendeeCount_ReturnsCorrectCount() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(rsvpRepository.countByEvent(testEvent)).thenReturn(15L);

        // Act
        Long count = rsvpService.getAttendeeCount(1L);

        // Assert
        assertEquals(15L, count);
        // Verify
        verify(rsvpRepository, times(1)).countByEvent(testEvent);
    }

    @Test
    void getAttendeeCount_ReturnsZero_WhenEventNotFound() {
        // Arrange
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Long count = rsvpService.getAttendeeCount(999L);

        // Assert
        assertEquals(0L, count);
    }

    @Test
    void createRSVP_Success_WhenBlockedRSVPDoesNotExist() {
        // Arrange
        when(rsvpRepository.existsByUserAndEvent(testUser, testEvent)).thenReturn(false);
        when(rsvpRepository.countByEvent(testEvent)).thenReturn(10L); // Under capacity

        // Act
        assertDoesNotThrow(() -> rsvpService.createRSVP(testUser, testEvent));

        // Assert
        verify(rsvpRepository, times(1)).save(any(RSVP.class));
    }

    @Test
    void createRSVP_ThrowsException_WhenUserIsBlocked() {
        // Arrange
        when(blockedRSVPRepository.existsByEventAndUser(testEvent, testUser)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> rsvpService.createRSVP(testUser, testEvent));

        assertEquals("You are blocked from RSVPing to this event", exception.getMessage());
        verify(rsvpRepository, never()).save(any(RSVP.class));
    }

    // ============== createRSVP() - Payment Scenarios ==============

    @Test
    void createRSVP_PaidEvent_SetsPaymentStatusPending() {
        testEvent.setRequiresPayment(true);
        testEvent.setPrice(new java.math.BigDecimal("25.00"));
        when(blockedRSVPRepository.existsByEventAndUser(testEvent, testUser)).thenReturn(false);
        when(rsvpRepository.existsByUserAndEvent(testUser, testEvent)).thenReturn(false);
        when(rsvpRepository.countByEvent(testEvent)).thenReturn(10L);
        when(rsvpRepository.save(any(RSVP.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RSVP result = rsvpService.createRSVP(testUser, testEvent);

        assertNotNull(result);
        assertEquals("pending", result.getPaymentStatus());
        verify(rsvpRepository).save(any(RSVP.class));
    }

    @Test
    void createRSVP_FreeEvent_NoPaymentStatus() {
        testEvent.setRequiresPayment(false);
        testEvent.setPrice(null);
        when(blockedRSVPRepository.existsByEventAndUser(testEvent, testUser)).thenReturn(false);
        when(rsvpRepository.existsByUserAndEvent(testUser, testEvent)).thenReturn(false);
        when(rsvpRepository.countByEvent(testEvent)).thenReturn(10L);
        when(rsvpRepository.save(any(RSVP.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RSVP result = rsvpService.createRSVP(testUser, testEvent);

        assertNotNull(result);
        assertNull(result.getPaymentStatus());
        verify(rsvpRepository).save(any(RSVP.class));
    }

    // ============== cancelRSVP() - Paid Event Logic ==============

    @Test
    void cancelRSVP_PaidEventWithPayment_CreatesCancelledRecord() {
        testEvent.setRequiresPayment(true);
        RSVP existingRSVP = new RSVP(testUser, testEvent);
        existingRSVP.setId(100L);
        existingRSVP.setPaymentStatus("paid");
        existingRSVP.setAmountPaid(new java.math.BigDecimal("25.00"));
        existingRSVP.setStripePaymentIntentId("pi_123");

        when(rsvpRepository.findByUser_UsernameAndEvent_Id("john.doe", 1L))
                .thenReturn(Optional.of(existingRSVP));

        rsvpService.cancelRSVP(1L, "john.doe");

        verify(cancelledRSVPRepository).save(any(au.edu.rmit.sept.webapp.model.CancelledRSVP.class));
        verify(paymentRepository).deleteByRsvp(existingRSVP);
        verify(rsvpRepository).delete(existingRSVP);
    }

    @Test
    void cancelRSVP_FreeEvent_NoCancelledRecord() {
        testEvent.setRequiresPayment(false);
        RSVP existingRSVP = new RSVP(testUser, testEvent);
        existingRSVP.setId(100L);

        when(rsvpRepository.findByUser_UsernameAndEvent_Id("john.doe", 1L))
                .thenReturn(Optional.of(existingRSVP));

        rsvpService.cancelRSVP(1L, "john.doe");

        verify(cancelledRSVPRepository, never()).save(any());
        verify(paymentRepository).deleteByRsvp(existingRSVP);
        verify(rsvpRepository).delete(existingRSVP);
    }

    // ============== blockUserFromEventAsOrganiser() ==============

    @Test
    void blockUserFromEventAsOrganiser_Success_OrganizerBlocks() {
        User organizer = new User();
        organizer.setId(2L);
        organizer.setUsername("jane.smith");
        testEvent.setCreatedBy(organizer);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(organizer));
        when(blockedRSVPRepository.existsByEventAndUser(testEvent, testUser)).thenReturn(false);
        when(rsvpRepository.findByUserAndEvent(testUser, testEvent)).thenReturn(Optional.empty());

        rsvpService.blockUserFromEventAsOrganiser(1L, 1L, 2L);

        verify(blockedRSVPRepository).save(any(au.edu.rmit.sept.webapp.model.BlockedRSVP.class));
    }

    @Test
    void blockUserFromEventAsOrganiser_Success_AdminBlocks() {
        User organizer = new User();
        organizer.setId(2L);
        User admin = new User();
        admin.setId(3L);
        testEvent.setCreatedBy(organizer);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(admin));
        when(userService.hasRole(3L, "ROLE_ADMIN")).thenReturn(true);
        when(blockedRSVPRepository.existsByEventAndUser(testEvent, testUser)).thenReturn(false);
        when(rsvpRepository.findByUserAndEvent(testUser, testEvent)).thenReturn(Optional.empty());

        rsvpService.blockUserFromEventAsOrganiser(1L, 1L, 3L);

        verify(blockedRSVPRepository).save(any(au.edu.rmit.sept.webapp.model.BlockedRSVP.class));
    }

    @Test
    void blockUserFromEventAsOrganiser_Failure_NonOrganizerNonAdmin() {
        User organizer = new User();
        organizer.setId(2L);
        User otherUser = new User();
        otherUser.setId(99L);
        testEvent.setCreatedBy(organizer);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(99L)).thenReturn(Optional.of(otherUser));
        when(userService.hasRole(99L, "ROLE_ADMIN")).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> rsvpService.blockUserFromEventAsOrganiser(1L, 1L, 99L));

        verify(blockedRSVPRepository, never()).save(any());
    }

    @Test
    void blockUserFromEventAsOrganiser_Failure_AlreadyBlocked() {
        User organizer = new User();
        organizer.setId(2L);
        testEvent.setCreatedBy(organizer);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(organizer));
        when(blockedRSVPRepository.existsByEventAndUser(testEvent, testUser)).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> rsvpService.blockUserFromEventAsOrganiser(1L, 1L, 2L));

        verify(blockedRSVPRepository, never()).save(any());
    }

    @Test
    void blockUserFromEventAsOrganiser_WithExistingRSVP_PaidEvent_CreatesCancelledRecord() {
        User organizer = new User();
        organizer.setId(2L);
        testEvent.setCreatedBy(organizer);
        testEvent.setRequiresPayment(true);

        RSVP existingRSVP = new RSVP(testUser, testEvent);
        existingRSVP.setId(100L);
        existingRSVP.setPaymentStatus("paid");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(organizer));
        when(blockedRSVPRepository.existsByEventAndUser(testEvent, testUser)).thenReturn(false);
        when(rsvpRepository.findByUserAndEvent(testUser, testEvent)).thenReturn(Optional.of(existingRSVP));

        rsvpService.blockUserFromEventAsOrganiser(1L, 1L, 2L);

        verify(cancelledRSVPRepository).save(any(au.edu.rmit.sept.webapp.model.CancelledRSVP.class));
        verify(paymentRepository).deleteByRsvp(existingRSVP);
        verify(rsvpRepository).delete(existingRSVP);
        verify(blockedRSVPRepository).save(any(au.edu.rmit.sept.webapp.model.BlockedRSVP.class));
    }

    @Test
    void blockUserFromEventAsOrganiser_WithExistingRSVP_FreeEvent_NoCancelledRecord() {
        User organizer = new User();
        organizer.setId(2L);
        testEvent.setCreatedBy(organizer);
        testEvent.setRequiresPayment(false);

        RSVP existingRSVP = new RSVP(testUser, testEvent);
        existingRSVP.setId(100L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(organizer));
        when(blockedRSVPRepository.existsByEventAndUser(testEvent, testUser)).thenReturn(false);
        when(rsvpRepository.findByUserAndEvent(testUser, testEvent)).thenReturn(Optional.of(existingRSVP));

        rsvpService.blockUserFromEventAsOrganiser(1L, 1L, 2L);

        verify(cancelledRSVPRepository, never()).save(any());
        verify(paymentRepository).deleteByRsvp(existingRSVP);
        verify(rsvpRepository).delete(existingRSVP);
        verify(blockedRSVPRepository).save(any(au.edu.rmit.sept.webapp.model.BlockedRSVP.class));
    }

    // ============== unblockUserFromEventAsOrganiser() ==============

    @Test
    void unblockUserFromEventAsOrganiser_Success_OrganizerUnblocks() {
        User organizer = new User();
        organizer.setId(2L);
        testEvent.setCreatedBy(organizer);

        au.edu.rmit.sept.webapp.model.BlockedRSVP blockedRSVP = new au.edu.rmit.sept.webapp.model.BlockedRSVP(testEvent, testUser, organizer);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(blockedRSVPRepository.findByEvent_IdAndUser_Id(1L, 1L)).thenReturn(Optional.of(blockedRSVP));

        rsvpService.unblockUserFromEventAsOrganiser(1L, 1L, 2L);

        verify(blockedRSVPRepository).delete(blockedRSVP);
    }

    @Test
    void unblockUserFromEventAsOrganiser_Success_AdminUnblocks() {
        User organizer = new User();
        organizer.setId(2L);
        User admin = new User();
        admin.setId(3L);
        testEvent.setCreatedBy(organizer);

        au.edu.rmit.sept.webapp.model.BlockedRSVP blockedRSVP = new au.edu.rmit.sept.webapp.model.BlockedRSVP(testEvent, testUser, organizer);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userService.hasRole(3L, "ROLE_ADMIN")).thenReturn(true);
        when(blockedRSVPRepository.findByEvent_IdAndUser_Id(1L, 1L)).thenReturn(Optional.of(blockedRSVP));

        rsvpService.unblockUserFromEventAsOrganiser(1L, 1L, 3L);

        verify(blockedRSVPRepository).delete(blockedRSVP);
    }

    @Test
    void unblockUserFromEventAsOrganiser_Failure_NonOrganizerNonAdmin() {
        User organizer = new User();
        organizer.setId(2L);
        User otherUser = new User();
        otherUser.setId(99L);
        testEvent.setCreatedBy(organizer);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userService.hasRole(99L, "ROLE_ADMIN")).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> rsvpService.unblockUserFromEventAsOrganiser(1L, 1L, 99L));

        verify(blockedRSVPRepository, never()).delete(any());
    }

    @Test
    @Disabled("Bug: Organizer can block themselves from their own event")
    void blockUserFromEventAsOrganiser_OrganizerCannotBlockSelf_ThrowsException() {
        // Setup: Organizer tries to block themselves from their own event
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(2L)).thenReturn(Optional.of(organizer)); // userId = organizerId
        when(blockedRSVPRepository.existsByEventAndUser(testEvent, organizer)).thenReturn(false);

        // This test will FAIL because the current implementation allows organizers to block themselves
        // Expected behavior: Should throw IllegalArgumentException when organizer tries to block themselves
        assertThrows(IllegalArgumentException.class, () -> {
            rsvpService.blockUserFromEventAsOrganiser(1L, 2L, 2L); // eventId, userId=organizerId, organizerId
        });

        // Verify no block record was created
        verify(blockedRSVPRepository, never()).save(any());
    }
}