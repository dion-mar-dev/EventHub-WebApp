package au.edu.rmit.sept.webapp.config;

import com.stripe.Stripe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/*
 * ⏺ Stripe test card details:

  Card number(successful payment): 4242 4242 4242 4242
  Expiry: Any future date (e.g., 12/34)
  CVC: Any 3 digits (e.g., 123)
  ZIP: Any 5 digits (e.g., 12345)

  Other test cards:
  - Decline card: 4000 0000 0000 0002
  - 3D Secure (requires auth): 4000 0025 0000 3155

  All test cards work with any CVC, expiry date (future), and postal code in Stripe test mode.
 */

/**
 * Configuration class for Stripe payment integration.
 * Initializes the Stripe SDK with API keys from application properties.
 */
@Configuration
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String apiKey;

    @Value("${stripe.publishable.key}")
    private String publishableKey;

    /**
     * Initialize Stripe SDK with secret API key on application startup.
     * This runs once when the Spring context is created.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
        System.out.println("✓ Stripe SDK initialized");
    }

    /**
     * Get publishable key for frontend use.
     * Publishable key is safe to expose in client-side code.
     */
    public String getPublishableKey() {
        return publishableKey;
    }
}