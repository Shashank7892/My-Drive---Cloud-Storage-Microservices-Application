package Mydrive.com.payment_service.controller;

import Mydrive.com.payment_service.dto.PaymentOrderRequest;
import Mydrive.com.payment_service.dto.PaymentOrderResponse;
import Mydrive.com.payment_service.model.PaymentTransactions;
import Mydrive.com.payment_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest; // For getting raw request body
import jakarta.validation.Valid; // For validating DTOs
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    private Long getUserId(Authentication authentication) {
        System.out.println("Authentication object: " + authentication);

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated.");
        }

        // Correctly extract userId from the Jwt principal
        if (authentication.getPrincipal() instanceof Jwt jwtPrincipal) {
            Long userIdInteger = jwtPrincipal.getClaim("userid");
            System.out.println("Extracted userId from JWT: " + userIdInteger);

            if (userIdInteger == null) {
                // If 'userid' claim is missing, it's a misconfigured token or a security issue
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User ID claim ('userid') not found in JWT.");
            }
            return userIdInteger.longValue(); // Convert to Long
        } else {
            // This case should ideally not happen with proper JWT authentication setup
            System.err.println("Authentication principal is not a JWT: " + authentication.getPrincipal().getClass().getName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication principal is not a JWT. Cannot extract user ID.");
        }
    }

    @PostMapping("/order")
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(@Valid @RequestBody PaymentOrderRequest request,
                                                                   Authentication authentication) {
        Long userId = getUserId(authentication);
        PaymentOrderResponse response = paymentService.createPaymentOrder(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK) // Always return 200 OK to Razorpay if webhook is received
    public ResponseEntity<String> handleRazorpayWebhook(HttpServletRequest request,
                                                        @RequestHeader("X-Razorpay-Signature") String razorpaySignature) {
        try {
            // Read raw request body for signature verification
            String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

            // Log incoming webhook for debugging (in production, use proper logging framework)
            System.out.println("Received Razorpay webhook. Event: " + payload);
            System.out.println("Signature: " + razorpaySignature);

            boolean webhookProcessed = paymentService.handleWebhook(payload, razorpaySignature);

            if (webhookProcessed) {
                return ResponseEntity.ok("Webhook processed successfully.");
            } else {
                // Return 200 OK to Razorpay even if internal processing fails
                // to prevent them from retrying excessively. Log the actual error internally.
                System.err.println("Internal processing failed for webhook, but acknowledging to Razorpay to prevent retries.");
                return ResponseEntity.status(HttpStatus.OK).body("Webhook received, but internal processing failed. See service logs.");
            }
        } catch (IOException e) {
            System.err.println("Error reading webhook payload: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading payload.");
        } catch (Exception e) {
            System.err.println("Unhandled error during webhook processing: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for unhandled exceptions
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unhandled error during webhook processing.");
        }
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<String> initiateRefund(@PathVariable Long paymentId,
                                                 @RequestParam @Min(value = 1, message = "Refund amount must be at least 1") Long refundAmount,
                                                 Authentication authentication) {
        Long userId = getUserId(authentication);
        paymentService.initiateRefund(paymentId, userId, refundAmount);
        return ResponseEntity.ok("Refund initiated successfully.");
    }


    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentTransactions> getPaymentDetails(@PathVariable Long paymentId,
                                                                 Authentication authentication) {
        Long userId = getUserId(authentication);
        PaymentTransactions payment = paymentService.getPaymentDetails(paymentId, userId);
        return ResponseEntity.ok(payment);
    }
}
