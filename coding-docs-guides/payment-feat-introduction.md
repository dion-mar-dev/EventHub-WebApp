# Payment Feature Implementation Guide

## Overview

The Campus Events Management System now supports **paid events** using **Stripe Checkout**. This document explains the implementation approach, setup requirements, and testing procedures.

---

## Architecture & Approach

### Why Stripe Checkout?

We chose **Stripe Checkout** (hosted payment page) over Stripe Elements (embedded forms) because:

- **Security**: Stripe handles all sensitive card data - we never see or store card numbers
- **Less Code**: Stripe provides the entire payment UI - we just redirect users
- **Mobile-Friendly**: Stripe's page is already optimized for all devices
- **Ideal for MVP**: Perfect complexity this level

### Payment Flow

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   User      │      │  Our Server  │      │   Stripe    │
└─────────────┘      └──────────────┘      └─────────────┘
      │                      │                      │
      │  1. Click RSVP       │                      │
      ├─────────────────────>│                      │
      │                      │                      │
      │  2. RSVP Created     │                      │
      │     (status=pending) │                      │
      │<─────────────────────┤                      │
      │                      │                      │
      │  3. Click "Pay Now"  │                      │
      ├─────────────────────>│                      │
      │                      │  4. Create Checkout  │
      │                      │      Session         │
      │                      ├─────────────────────>│
      │                      │                      │
      │                      │  5. Checkout URL     │
      │                      │<─────────────────────┤
      │                      │                      │
      │  6. Redirect to Stripe Checkout             │
      │<─────────────────────┤                      │
      │                      │                      │
      │  7. Enter Card & Pay │                      │
      ├──────────────────────┼─────────────────────>│
      │                      │                      │
      │  8. Payment Success  │                      │
      │<─────────────────────┼──────────────────────┤
      │                      │                      │
      │  9. Redirect to Event│                      │
      │     Details Page     │                      │
      │<─────────────────────┤                      │
      │                      │                      │
      │                      │  10. Webhook Event   │
      │                      │      (async)         │
      │                      │<─────────────────────┤
      │                      │                      │
      │                      │  11. Update RSVP     │
      │                      │      status=paid     │
      │                      │                      │
      │  12. Auto-refresh or │                      │
      │      manual reload   │                      │
      ├─────────────────────>│                      │
      │                      │                      │
      │  13. "Paid ✓" badge  │                      │
      │<─────────────────────┤                      │
```

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| **StripeConfig** | `config/StripeConfig.java` | Initializes Stripe API with secret key |
| **StripeService** | `service/StripeService.java` | Creates checkout sessions, processes webhooks |
| **PaymentController** | `controller/PaymentController.java` | Handles payment requests and webhook endpoint |
| **RSVPService** | `service/RSVPService.java` | Creates RSVPs with `payment_status = 'pending'` |
| **Event Model** | `model/Event.java` | Added `price` and `requiresPayment` fields |
| **RSVP Model** | `model/RSVP.java` | Added `paymentStatus`, `stripePaymentIntentId`, `amountPaid` |
| **Payment Model** | `model/Payment.java` | New table to log all payment transactions |
| **SecurityConfig** | `config/SecurityConfig.java` | Webhook endpoint is public (line 72, 136) |

---

## Database Changes

### Event Table
```sql
price DECIMAL(10,2),              -- Event ticket price
requires_payment BOOLEAN DEFAULT FALSE
```

### RSVP Table
```sql
payment_status VARCHAR(20),           -- 'pending' or 'paid'
stripe_payment_intent_id VARCHAR(255),
amount_paid DECIMAL(10,2)
```

### Payments Table (New)
```sql
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rsvp_id BIGINT NOT NULL,
    stripe_payment_intent_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rsvp_id) REFERENCES rsvp(id)
);
```

**Purpose**: Maintains a permanent audit log of all payment transactions separate from the RSVP records.

---

## Setup Instructions

### 1. Stripe Account Configuration

The Stripe API keys are **already configured** in `application.properties`:

```properties
stripe.api.key=sk_test_xxxxx            # Secret key (backend)
stripe.publishable.key=pk_test_xxxxx    # Publishable key (frontend, if needed)
stripe.webhook.secret=whsec_xxxxx       # Webhook signing secret
```

⚠️ **Normally wouldn't commit these keys to Git** - usually add to `.gitignore` or environment variables for production, but these are just sandbox test keys.

#### Using Your Own Stripe Account
To use your own Stripe account instead: \
(1) Create/login to your Stripe account, \
(2) Get API keys from Stripe Dashboard → Developers → API keys, \
(3) Update `application.properties` with `stripe.api.key` (secret key) and `stripe.publishable.key` (publishable key), \
(4) Run `stripe listen --forward-to localhost:8080/api/payments/webhook`, \
(5) Copy webhook secret from CLI output (shows `whsec_...`), \
(6) Update `stripe.webhook.secret` in `application.properties`, \
(7) Restart app. 

### 2. Install Stripe CLI (Required for Local Development)

The Stripe CLI is needed to forward webhook events from Stripe's servers to your local machine.

#### macOS
```bash
brew install stripe/stripe-cli/stripe
```

#### Windows
```bash
scoop bucket add stripe https://github.com/stripe/scoop-stripe-cli.git
scoop install stripe
```

#### Linux
```bash
wget https://github.com/stripe/stripe-cli/releases/latest/download/stripe_X.X.X_linux_x86_64.tar.gz
tar -xvf stripe_X.X.X_linux_x86_64.tar.gz
sudo mv stripe /usr/local/bin/
```

Verify installation:
```bash
stripe --version
```

### 3. Login to Stripe CLI

```bash
stripe login
```

This opens a browser window to authenticate with your Stripe account.

### 4. Start Webhook Forwarding (Critical!)

**Before starting the application**, run this command in a **separate terminal**:

```bash
stripe listen --forward-to localhost:8080/api/payments/webhook
```

**Why is this needed?**

- Stripe's servers are on the internet, but your local dev server (`localhost:8080`) is not publicly accessible
- When a payment succeeds, Stripe needs to send a webhook to notify your app
- The Stripe CLI creates a **secure tunnel** that forwards webhook events from Stripe → your localhost
- **Without this, payments will complete in Stripe but your app won't know about them** (RSVP status stays "pending" forever)

**What you'll see:**
```
> Ready! Your webhook signing secret is whsec_1234... (^C to quit)
> Forwarding all events to localhost:8080/api/payments/webhook
```

⚠️ **Keep this terminal running** while testing payments.

### 5. Start the Application

#### Option A: Running with Docker (Recommended)

**Terminal 1 - Stripe CLI:**
```bash
stripe listen --forward-to localhost:8080/api/payments/webhook
```

**Terminal 2 - Docker Containers:**
```bash
# Local development (builds from source)
docker compose up

# OR remote testing (pulls from Docker Hub)
docker compose -f docker-compose.remote.yml up
```

**Why separate terminals?**
- Stripe CLI requires authentication on your host machine
- Docker containers are isolated and cannot access host Stripe credentials
- Containers would need Stripe CLI installed + authentication configured (overcomplicated)
- Keeping them separate is simpler and follows Stripe's documented approach

#### Option B: Running without Docker

```bash
mvn clean spring-boot:run -Dmaven.test.skip=true
```

Or run via your IDE, but make sure tests aren't running.

---

## How It Works: Step-by-Step

### User Journey

1. **Create a Paid Event**
   - Navigate to `/events/create`
   - Enter event details
   - Check "Requires Payment" checkbox
   - Enter price (e.g., `25.00`)
   - Submit form

2. **User RSVPs to Paid Event**
   - Click "RSVP Now" button
   - RSVP is created with `payment_status = 'pending'`
   - User sees green "You're Going!" card with a **"Pay Now"** button

3. **User Clicks "Pay Now"**
   - Form submits to `/api/payments/create-checkout-session`
   - `PaymentController` calls `StripeService.createCheckoutSession()`
   - Stripe API returns a checkout URL
   - User is redirected to Stripe's hosted payment page

4. **User Completes Payment**
   - User enters card details on Stripe's page
   - Stripe processes payment
   - On success: Stripe redirects to `/events/{id}?payment=success`
   - On cancel: Stripe redirects to `/events/{id}?payment=cancelled`

5. **Webhook Updates Payment Status (Asynchronous)**
   - Stripe sends `checkout.session.completed` webhook to `/api/payments/webhook`
   - `StripeService.handleWebhookEvent()` verifies webhook signature
   - Updates RSVP: `payment_status = 'paid'`
   - Creates `Payment` record for audit trail

6. **User Sees Confirmation**
   - If webhook processed: Shows "Payment successful! Your RSVP is confirmed."
   - If webhook delayed: Shows "Payment processing..." with 3-second auto-refresh
   - Once updated: "Pay Now" button changes to "Paid ✓" badge

---

## Code Reference Points

### Creating a Checkout Session
**File**: `StripeService.java:52-93`
```java
public String createCheckoutSession(Long eventId, Long userId, Long rsvpId) {
    // Builds Stripe checkout session with event price
    // Returns hosted checkout URL
}
```

### Webhook Processing
**File**: `StripeService.java:102-129`
```java
public void handleWebhookEvent(String payload, String sigHeader) {
    // Verifies webhook signature
    // Processes checkout.session.completed event
    // Updates RSVP payment status
}
```

### Security Configuration
**File**: `SecurityConfig.java:72,136`
```java
.requestMatchers("/api/payments/webhook").permitAll();  // Line 72
csrf.ignoringRequestMatchers("/api/payments/webhook"); // Line 136
```

**Why?**
- Webhook endpoint must be **publicly accessible** (Stripe servers can't authenticate)
- **CSRF exemption** needed (Stripe can't send CSRF tokens)
- **Signature verification** in code provides security instead

---

## Testing the Payment Feature

### Test a Successful Payment

1. Ensure Stripe CLI is running: `stripe listen --forward-to localhost:8080/api/payments/webhook`
2. Create a paid event (price: $25.00)
3. RSVP to the event (logged in as test user)
4. Click "Pay Now"
5. On Stripe checkout page, use test card: `4242 4242 4242 4242`
6. Complete payment
7. You should be redirected back with "Payment successful!" message
8. Check the Stripe CLI terminal - you should see the webhook event logged

### Test a Declined Payment

- Use card number: `4000 0000 0000 0002`
- Payment will be declined by Stripe

### Test Webhook Delays

- If you see "Payment processing..." message, the webhook hasn't arrived yet
- Page auto-refreshes every 3 seconds
- Check Stripe CLI output for webhook delivery

---

## Common Issues & Troubleshooting

### Issue: "Payment processing..." never updates to "Paid"

**Cause**: Stripe CLI not running or webhook not forwarded

**Fix**:
```bash
stripe listen --forward-to localhost:8080/api/payments/webhook
```

### Issue: Webhook returns 400 Bad Request

**Cause**: Webhook signature verification failed

**Fix**: Update `stripe.webhook.secret` in `application.properties` with the secret from Stripe CLI output

### Issue: Can't access `/api/payments/webhook` endpoint

**Cause**: Spring Security blocking the request

**Fix**: Verify `SecurityConfig.java` has webhook endpoint in `permitAll()` and CSRF exemption

---

## Production Deployment Notes

### Webhook Configuration

In production, you **don't use Stripe CLI**. Instead:

1. Go to Stripe Dashboard → Developers → Webhooks
2. Add endpoint URL: `https://yourdomain.com/api/payments/webhook`
3. Select events to listen for: `checkout.session.completed`
4. Copy the webhook signing secret to your production environment variables

### Environment Variables

Replace hardcoded keys in `application.properties` with environment variables:

```properties
stripe.api.key=${STRIPE_SECRET_KEY}
stripe.webhook.secret=${STRIPE_WEBHOOK_SECRET}
```

### Success/Cancel URLs

Update hardcoded URLs in `StripeService.java:68-69`:

```java
.setSuccessUrl("https://yourdomain.com/events/" + eventId + "?payment=success")
.setCancelUrl("https://yourdomain.com/events/" + eventId + "?payment=cancelled")
```

---

## ⏺ Stripe Test Card Details

### Successful Payment
- **Card number**: `4242 4242 4242 4242`
- **Expiry**: Any future date (e.g., `12/34`)
- **CVC**: Any 3 digits (e.g., `123`)
- **ZIP**: Any 5 digits (e.g., `12345`)

### Other Test Cards
- **Decline card**: `4000 0000 0000 0002`
- **3D Secure (requires auth)**: `4000 0025 0000 3155`

**All test cards work with any CVC, expiry date (future), and postal code in Stripe test mode.**

---

## References

- [Stripe Checkout Documentation](https://stripe.com/docs/payments/checkout)
- [Stripe CLI Documentation](https://stripe.com/docs/stripe-cli)
- [Stripe Test Cards](https://stripe.com/docs/testing#cards)
- [Stripe Webhooks](https://stripe.com/docs/webhooks)

---

**Last Updated**: 2025-10-05
**Feature Branch**: `feat/payments`
