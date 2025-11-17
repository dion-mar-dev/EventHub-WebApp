package au.edu.rmit.sept.webapp.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.net.Webhook;

import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.model.Payment;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.repository.PaymentRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for handling Stripe payment operations.
 * Manages checkout session creation and webhook event processing.
 */
@Service
public class StripeService {

    private final RSVPRepository rsvpRepository;
    private final PaymentRepository paymentRepository;
    private final EventRepository eventRepository;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    public StripeService(RSVPRepository rsvpRepository,
            PaymentRepository paymentRepository,
            EventRepository eventRepository) {
        this.rsvpRepository = rsvpRepository;
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Creates a Stripe Checkout session for a paid event.
     * 
     * @param eventId The event to pay for
     * @param userId  The user making the payment
     * @param rsvpId  The RSVP record to associate with payment
     * @return Checkout session URL to redirect user to
     */
    public String createCheckoutSession(Long eventId, Long userId, Long rsvpId) throws StripeException {
        // Fetch event details
        au.edu.rmit.sept.webapp.model.Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        // Verify event requires payment
        if (!event.getRequiresPayment() || event.getPrice() == null) {
            throw new IllegalArgumentException("Event does not require payment");
        }

        // Convert price to cents (Stripe uses smallest currency unit)
        long amountInCents = event.getPrice().multiply(new BigDecimal("100")).longValue();

        // Build checkout session parameters
        // Old URLs (use when running locally without public URL): http://localhost:8080/events/...
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(baseUrl + "/events/" + eventId + "?payment=success")
                .setCancelUrl(baseUrl + "/events/" + eventId + "?payment=cancelled")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("aud")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(event.getTitle())
                                                                .setDescription("Event ticket for " + event.getTitle())
                                                                .build())
                                                .build())
                                .setQuantity(1L)
                                .build())
                .putMetadata("eventId", eventId.toString())
                .putMetadata("userId", userId.toString())
                .putMetadata("rsvpId", rsvpId.toString())
                .build();

        // Create the session
        Session session = Session.create(params);

        return session.getUrl();
    }

    /**
     * Handles incoming Stripe webhook events.
     * Verifies webhook signature and processes payment completion.
     * 
     * @param payload   Raw webhook payload
     * @param sigHeader Stripe signature header
     */
    @Transactional
    public void handleWebhookEvent(String payload, String sigHeader) {
        Event event;

        // Verify webhook signature (if secret is configured)
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            try {
                event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid webhook signature");
            }
        } else {
            // Development mode: skip signature verification
            try {
                event = Event.GSON.fromJson(payload, Event.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid webhook payload");
            }
        }

        // Handle payment success event
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getData().getObject();
            if (session != null && "paid".equals(session.getPaymentStatus())) {
                processSuccessfulPayment(session);
            }
        }
    }

    /**
     * Process a successful payment by updating RSVP and creating payment record.
     */
    private void processSuccessfulPayment(Session session) {
        // Extract metadata
        Long rsvpId = Long.parseLong(session.getMetadata().get("rsvpId"));
        String paymentIntentId = session.getPaymentIntent();

        // Update RSVP payment status
        RSVP rsvp = rsvpRepository.findById(rsvpId)
                .orElseThrow(() -> new IllegalArgumentException("RSVP not found"));

        rsvp.setPaymentStatus("paid");
        rsvp.setStripePaymentIntentId(paymentIntentId);
        rsvp.setAmountPaid(new BigDecimal(session.getAmountTotal()).divide(new BigDecimal("100")));
        rsvpRepository.save(rsvp);

        // Create payment record
        Payment payment = new Payment(
                rsvp,
                paymentIntentId,
                rsvp.getAmountPaid(),
                "paid");
        paymentRepository.save(payment);
    }

    /**
     * Processes a refund for a Stripe payment.
     *
     * @param paymentIntentId The Stripe payment intent ID to refund
     * @param amount The amount to refund (in dollars)
     * @return The Stripe refund ID
     * @throws StripeException if refund fails
     */
    public String refundPayment(String paymentIntentId, BigDecimal amount) throws StripeException {
        // Convert amount to cents
        long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

        // Create refund parameters
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(amountInCents)
                .build();

        // Process refund via Stripe API
        Refund refund = Refund.create(params);

        return refund.getId();
    }
}