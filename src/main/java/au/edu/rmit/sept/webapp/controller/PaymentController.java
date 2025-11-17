package au.edu.rmit.sept.webapp.controller;

import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.service.StripeService;
import au.edu.rmit.sept.webapp.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for handling Stripe payment operations.
 *
 * Purpose:
 * - Creates Stripe checkout sessions for paid events
 * - Receives asynchronous notifications from Stripe when payment events occur
 * - Acts as the callback endpoint after users complete payment on Stripe's hosted page
 * - Updates RSVP payment status based on Stripe's webhook notifications
 *
 * Security:
 * - Webhook endpoint is publicly accessible (configured in SecurityConfig)
 * - Webhook signature verification prevents unauthorized requests
 * - No sensitive error details exposed to external callers
 */
@Controller
@RequestMapping("/api/payments")
public class PaymentController {

    // StripeService handles webhook verification and payment processing logic
    private final StripeService stripeService;
    private final UserService userService;

    public PaymentController(StripeService stripeService, UserService userService) {
        this.stripeService = stripeService;
        this.userService = userService;
    }

    /**
     * Creates a Stripe checkout session and redirects user to payment page.
     *
     * Flow:
     * 1. User clicks "Pay Now" button on event details page
     * 2. Form posts eventId and rsvpId to this endpoint
     * 3. Create Stripe checkout session with event/user details
     * 4. Redirect user to Stripe's hosted checkout page
     * 5. After payment, Stripe redirects back to event page
     * 6. Webhook updates payment status asynchronously
     *
     * @param eventId The event being paid for
     * @param rsvpId The RSVP record to associate with payment
     * @param authentication Current logged-in user
     * @param redirectAttributes Flash attributes for error messages
     * @return Redirect to Stripe checkout page or back to event page on error
     */
    @PostMapping("/create-checkout-session")
    public String createCheckoutSession(
            @RequestParam Long eventId,
            @RequestParam Long rsvpId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            // Get current user
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Create Stripe checkout session
            String checkoutUrl = stripeService.createCheckoutSession(eventId, user.getId(), rsvpId);

            // Redirect to Stripe payment page
            return "redirect:" + checkoutUrl;

        } catch (Exception e) {
            // Handle errors and redirect back to event page
            redirectAttributes.addFlashAttribute("error",
                    "Failed to create payment session: " + e.getMessage());
            return "redirect:/events/" + eventId;
        }
    }

    /**
     * Webhook endpoint for Stripe payment events.
     *
     * Flow:
     * 1. User completes payment on Stripe's checkout page
     * 2. Stripe sends POST request to this endpoint with payment details
     * 3. Signature verification ensures request is genuinely from Stripe
     * 4. Payment status is updated in database (pending → paid)
     * 5. Return 200 to acknowledge receipt (Stripe stops retrying)
     *
     * @param payload Raw JSON payload from Stripe containing event data
     * @param sigHeader Stripe-Signature header for cryptographic verification
     * @return 200 OK if processed successfully
     *         400 Bad Request for invalid signature/payload (no retry)
     *         500 Internal Server Error for processing failures (Stripe retries)
     */
    @PostMapping("/webhook")
    @ResponseBody
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,              // Raw JSON - must not be parsed by Spring
            @RequestHeader("Stripe-Signature") String sigHeader) {  // HMAC signature from Stripe

        try {
            // Delegate to StripeService for signature verification and event processing
            // This updates RSVP.paymentStatus, creates Payment record, etc.
            stripeService.handleWebhookEvent(payload, sigHeader);

            // 200 OK tells Stripe: "received and processed, don't retry"
            return ResponseEntity.ok("Webhook processed successfully");

        } catch (IllegalArgumentException e) {
            // Invalid signature or malformed payload
            // 400 tells Stripe: "this webhook is bad, don't retry"
            System.err.println("Invalid webhook: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid webhook");

        } catch (Exception e) {
            // Database error, service unavailable, etc.
            // 500 tells Stripe: "temporary failure, please retry later"
            System.err.println("Webhook processing error: " + e.getMessage());
            return ResponseEntity.status(500).body("Processing error");
        }
    }
}

/**
 * IDEMPOTENCY DESIGN DECISION:
 *
 * This implementation does NOT include duplicate webhook detection/idempotency checks.
 *
 * Reasoning:
 * 1. Stripe webhooks are highly reliable - duplicates are rare in practice
 * 2. University assignment scope - focus on core payment flow over edge cases
 * 3. Database constraints provide partial protection:
 *    - RSVP table has unique constraint on (user_id, event_id)
 *    - Multiple updates to same RSVP's payment_status are safe (idempotent)
 * 4. If needed in production, track processed event IDs:
 *    - Add `processed_stripe_event_ids` table
 *    - Check if event.id exists before processing
 *    - Return 200 immediately for duplicates
 *
 * Current risk: Duplicate payments table entries if webhook fires twice.
 * Impact: Low - only creates redundant log entries, doesn't affect payment status.
 */
