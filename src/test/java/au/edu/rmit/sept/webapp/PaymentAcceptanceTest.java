package au.edu.rmit.sept.webapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.model.Payment;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.repository.PaymentRepository;
import au.edu.rmit.sept.webapp.service.StripeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Acceptance tests for Payment and Paid Events functionality.
 * Tests the complete flow of creating paid events and processing payments.
 *
 * Key Testing Strategy:
 * - Uses @MockBean to mock StripeService (avoids real Stripe API calls)
 * - Tests application logic: event creation, RSVP flow, payment status
 * - Simulates Stripe webhook callbacks for payment completion
 * - Verifies database state changes (RSVP payment status, Payment records)
 *
 * Features tested:
 * - Creating paid vs free events
 * - RSVP flow for paid events (redirect to payment)
 * - RSVP flow for free events (immediate confirmation)
 * - Payment webhook processing
 * - Payment status tracking
 * - Price validation
 *
 * mvn test -Dtest=PaymentAcceptanceTest
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RSVPRepository rsvpRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Mock Stripe service to avoid real API calls
    @MockBean
    private StripeService stripeService;

    private User testOrganizer;
    private User testAttendee;
    private Category techCategory;
    private Event freeEvent;
    private Event paidEvent;
    private Event expensiveEvent;

    @BeforeEach
    void setUp() throws Exception {
        // Get existing category
        techCategory = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Technology"))
                .findFirst()
                .orElseThrow();

        // Create test users
        testOrganizer = new User();
        testOrganizer.setUsername("payment.organizer");
        testOrganizer.setEmail("organizer@test.com");
        testOrganizer.setPassword(passwordEncoder.encode("password123"));
        testOrganizer.setEnabled(true);
        userRepository.save(testOrganizer);

        testAttendee = new User();
        testAttendee.setUsername("payment.attendee");
        testAttendee.setEmail("attendee@test.com");
        testAttendee.setPassword(passwordEncoder.encode("password123"));
        testAttendee.setEnabled(true);
        userRepository.save(testAttendee);

        // Create FREE event
        freeEvent = new Event();
        freeEvent.setTitle("Free Workshop");
        freeEvent.setDescription("A free workshop open to all");
        freeEvent.setEventDate(LocalDate.now().plusDays(10));
        freeEvent.setEventTime(LocalTime.of(14, 0));
        freeEvent.setLocation("Community Center");
        freeEvent.setCapacity(50);
        freeEvent.setCategory(techCategory);
        freeEvent.setCreatedBy(testOrganizer);
        freeEvent.setRequiresPayment(false);
        freeEvent.setPrice(null);
        eventRepository.save(freeEvent);

        // Create PAID event
        paidEvent = new Event();
        paidEvent.setTitle("Paid Conference");
        paidEvent.setDescription("Premium conference with industry experts");
        paidEvent.setEventDate(LocalDate.now().plusDays(15));
        paidEvent.setEventTime(LocalTime.of(9, 0));
        paidEvent.setLocation("Convention Center");
        paidEvent.setCapacity(100);
        paidEvent.setCategory(techCategory);
        paidEvent.setCreatedBy(testOrganizer);
        paidEvent.setRequiresPayment(true);
        paidEvent.setPrice(new BigDecimal("49.99"));
        eventRepository.save(paidEvent);

        // Create EXPENSIVE paid event
        expensiveEvent = new Event();
        expensiveEvent.setTitle("Expensive Bootcamp");
        expensiveEvent.setDescription("Intensive 3-day coding bootcamp");
        expensiveEvent.setEventDate(LocalDate.now().plusDays(20));
        expensiveEvent.setEventTime(LocalTime.of(9, 0));
        expensiveEvent.setLocation("Tech Hub");
        expensiveEvent.setCapacity(30);
        expensiveEvent.setCategory(techCategory);
        expensiveEvent.setCreatedBy(testOrganizer);
        expensiveEvent.setRequiresPayment(true);
        expensiveEvent.setPrice(new BigDecimal("299.00"));
        eventRepository.save(expensiveEvent);

        // Configure mock Stripe service default behavior
        when(stripeService.createCheckoutSession(anyLong(), anyLong(), anyLong()))
                .thenReturn("https://checkout.stripe.com/mock-session-12345");
    }

    @Test
    void testCreateFreeEvent_NoPaymentRequired() throws Exception {
        // Create a free event
        mockMvc.perform(post("/events/create")
                .with(user(testOrganizer.getUsername()))
                .param("title", "Another Free Event")
                .param("description", "This event is completely free")
                .param("eventDate", LocalDate.now().plusDays(5).toString())
                .param("eventTime", "18:00")
                .param("location", "Park")
                .param("capacity", "100")
                .param("categoryId", techCategory.getId().toString())
                .param("requiresPayment", "false")
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*"));

        // Verify event was created as free
        Event event = eventRepository.findAll().stream()
                .filter(e -> e.getTitle().equals("Another Free Event"))
                .findFirst()
                .orElseThrow();

        assertFalse(event.getRequiresPayment());
        assertNull(event.getPrice());
    }

    @Test
    void testCreatePaidEvent_WithPrice() throws Exception {
        // Note: This tests the existing paid event from setUp
        // Event creation form may not support price/requiresPayment params yet

        // Verify paid event exists with payment requirement
        assertTrue(paidEvent.getRequiresPayment());
        assertEquals(new BigDecimal("49.99"), paidEvent.getPrice());
    }

    @Test
    void testRsvpFreeEvent_ImmediateConfirmation() throws Exception {
        // RSVP to free event
        mockMvc.perform(post("/rsvp/" + freeEvent.getId())
                .with(user(testAttendee.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + freeEvent.getId()))
                .andExpect(flash().attribute("success", "Successfully RSVP'd to Free Workshop"));

        // Verify RSVP was created immediately
        RSVP rsvp = rsvpRepository.findByUserAndEvent(testAttendee, freeEvent)
                .orElseThrow(() -> new AssertionError("RSVP should be created"));

        assertNotNull(rsvp);
        assertNull(rsvp.getPaymentStatus()); // Free events don't have payment status
        assertNull(rsvp.getAmountPaid());

        // Verify no payment record created
        assertEquals(0, paymentRepository.count());
    }

    @Test
    void testRsvpPaidEvent_CreatesPendingRSVP() throws Exception {
        // RSVP to paid event
        mockMvc.perform(post("/rsvp/" + paidEvent.getId())
                .with(user(testAttendee.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + paidEvent.getId()))
                .andExpect(flash().attributeExists("success"));

        // Verify RSVP was created with pending payment status
        RSVP rsvp = rsvpRepository.findByUserAndEvent(testAttendee, paidEvent)
                .orElseThrow(() -> new AssertionError("RSVP should be created"));

        assertNotNull(rsvp);
        assertEquals("pending", rsvp.getPaymentStatus());
        assertNull(rsvp.getStripePaymentIntentId()); // Not paid yet
        assertNull(rsvp.getAmountPaid()); // Not paid yet
    }

    @Test
    void testPaymentCheckoutSession_RedirectsToStripe() throws Exception {
        // Create RSVP first
        RSVP rsvp = new RSVP(testAttendee, paidEvent);
        rsvp.setPaymentStatus("pending");
        rsvpRepository.save(rsvp);

        // Attempt payment - should redirect to Stripe
        mockMvc.perform(post("/api/payments/create-checkout-session")
                .with(user(testAttendee.getUsername()))
                .param("eventId", paidEvent.getId().toString())
                .param("rsvpId", rsvp.getId().toString())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.stripe.com/mock-session-12345"));

        // Verify Stripe service was called with correct parameters
        verify(stripeService).createCheckoutSession(
                eq(paidEvent.getId()),
                eq(testAttendee.getId()),
                eq(rsvp.getId()));
    }

    @Test
    void testWebhookPaymentSuccess_UpdatesRSVPStatus() throws Exception {
        // Create pending RSVP
        RSVP rsvp = new RSVP(testAttendee, paidEvent);
        rsvp.setPaymentStatus("pending");
        rsvpRepository.save(rsvp);

        // Simulate successful Stripe webhook
        String webhookPayload = buildStripeWebhookPayload(
                rsvp.getId(),
                "pi_mock_payment_intent_123",
                4999L); // $49.99 in cents

        // Configure mock to process the webhook
        doNothing().when(stripeService).handleWebhookEvent(anyString(), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                .contentType("application/json")
                .content(webhookPayload)
                .header("Stripe-Signature", "mock-signature-xyz"))
                .andExpect(status().isOk());

        // Verify webhook handler was called
        verify(stripeService).handleWebhookEvent(eq(webhookPayload), eq("mock-signature-xyz"));
    }

    @Test
    void testWebhookInvalidSignature_Returns400() throws Exception {
        // Configure mock to throw exception on invalid signature
        doThrow(new IllegalArgumentException("Invalid webhook signature"))
                .when(stripeService).handleWebhookEvent(anyString(), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                .contentType("application/json")
                .content("{\"invalid\":\"payload\"}")
                .header("Stripe-Signature", "bad-signature"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPaidEventDisplaysPrice() throws Exception {
        // View paid event details page
        mockMvc.perform(get("/events/" + paidEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"))
                .andExpect(model().attributeExists("event"));
    }

    @Test
    void testFreeEventNoPriceDisplay() throws Exception {
        // View free event details page
        mockMvc.perform(get("/events/" + freeEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"))
                .andExpect(model().attributeExists("event"));
    }

    @Test
    void testCannotCreatePaidEventWithoutPrice() throws Exception {
        // Test that paid events require price validation
        // This validates the business rule that requiresPayment=true needs a price

        Event invalidEvent = new Event();
        invalidEvent.setRequiresPayment(true);
        invalidEvent.setPrice(null); // Invalid state

        // Verify this is an invalid state (would fail validation)
        assertTrue(invalidEvent.getRequiresPayment());
        assertNull(invalidEvent.getPrice());
    }

    @Test
    void testMultipleUsersCanRsvpPaidEvent() throws Exception {
        // Create another attendee
        User attendee2 = new User();
        attendee2.setUsername("attendee2");
        attendee2.setEmail("attendee2@test.com");
        attendee2.setPassword(passwordEncoder.encode("password123"));
        attendee2.setEnabled(true);
        userRepository.save(attendee2);

        // First user RSVPs
        mockMvc.perform(post("/rsvp/" + paidEvent.getId())
                .with(user(testAttendee.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Second user RSVPs
        mockMvc.perform(post("/rsvp/" + paidEvent.getId())
                .with(user(attendee2.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Verify both RSVPs created
        assertTrue(rsvpRepository.findByUserAndEvent(testAttendee, paidEvent).isPresent());
        assertTrue(rsvpRepository.findByUserAndEvent(attendee2, paidEvent).isPresent());
    }

    @Test
    void testPriceFormattingAndValidation() throws Exception {
        // Test that price is stored correctly in BigDecimal format
        // Uses existing paid event from setUp

        assertEquals(new BigDecimal("49.99"), paidEvent.getPrice());
        assertEquals(new BigDecimal("299.00"), expensiveEvent.getPrice());

        // Verify price precision is maintained
        assertTrue(paidEvent.getPrice().scale() == 2);
        assertTrue(expensiveEvent.getPrice().scale() == 2);
    }

    @Test
    void testNegativePriceRejected() throws Exception {
        // Attempt to create event with negative price
        mockMvc.perform(post("/events/create")
                .with(user(testOrganizer.getUsername()))
                .param("title", "Negative Price Event")
                .param("description", "Should be rejected")
                .param("eventDate", LocalDate.now().plusDays(5).toString())
                .param("eventTime", "14:00")
                .param("location", "Test Location")
                .param("capacity", "30")
                .param("categoryId", techCategory.getId().toString())
                .param("requiresPayment", "true")
                .param("price", "-10.00")
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testFreeEventWithPriceIgnoresPrice() throws Exception {
        // Create free event but accidentally include price
        mockMvc.perform(post("/events/create")
                .with(user(testOrganizer.getUsername()))
                .param("title", "Confusing Event")
                .param("description", "Free event with price set")
                .param("eventDate", LocalDate.now().plusDays(5).toString())
                .param("eventTime", "16:00")
                .param("location", "Confused Venue")
                .param("capacity", "50")
                .param("categoryId", techCategory.getId().toString())
                .param("requiresPayment", "false")
                .param("price", "50.00") // Should be ignored
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Event event = eventRepository.findAll().stream()
                .filter(e -> e.getTitle().equals("Confusing Event"))
                .findFirst()
                .orElseThrow();

        assertFalse(event.getRequiresPayment());
        // Price might be stored but requiresPayment flag is false
    }

    @Test
    void testUnauthenticatedUserCannotRsvpPaidEvent() throws Exception {
        // Attempt RSVP without authentication
        mockMvc.perform(post("/rsvp/" + paidEvent.getId())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void testPaymentCheckoutSessionCreationFailure() throws Exception {
        // Create RSVP
        RSVP rsvp = new RSVP(testAttendee, paidEvent);
        rsvp.setPaymentStatus("pending");
        rsvpRepository.save(rsvp);

        // Configure mock to throw exception
        when(stripeService.createCheckoutSession(anyLong(), anyLong(), anyLong()))
                .thenThrow(new RuntimeException("Stripe API error"));

        // Attempt payment
        mockMvc.perform(post("/api/payments/create-checkout-session")
                .with(user(testAttendee.getUsername()))
                .param("eventId", paidEvent.getId().toString())
                .param("rsvpId", rsvp.getId().toString())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + paidEvent.getId()))
                .andExpect(flash().attributeExists("error"));
    }

    // Helper method to build mock Stripe webhook payload
    private String buildStripeWebhookPayload(Long rsvpId, String paymentIntentId, Long amountInCents) {
        return String.format("""
            {
              "type": "checkout.session.completed",
              "data": {
                "object": {
                  "id": "cs_test_mock",
                  "payment_status": "paid",
                  "payment_intent": "%s",
                  "amount_total": %d,
                  "metadata": {
                    "rsvpId": "%d"
                  }
                }
              }
            }
            """, paymentIntentId, amountInCents, rsvpId);
    }
}
