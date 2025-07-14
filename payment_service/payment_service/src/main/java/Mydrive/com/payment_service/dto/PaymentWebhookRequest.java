package Mydrive.com.payment_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class PaymentWebhookRequest {
    private String entity; // e.g., "event"
    private String account_id;
    private String event; // e.g., "payment.captured", "payment.failed"
    private int created_at; // Unix timestamp

    @JsonProperty("payload")
    private WebhookPayload payload;

    @Data
    public static class WebhookPayload {
        @JsonProperty("payment")
        private PaymentEntity payment;
        // Depending on the event, there might also be 'order', 'refund', etc.
        // Example: For 'payment.refunded', you'd have a 'refund' object here.
    }

    @Data
    public static class PaymentEntity {
        private String id; // Razorpay PaymentTransactions ID
        private String entity; // "payment"
        private Long amount; // in smallest unit (e.g., paisa)
        private String currency;
        private String status; // "captured", "failed", "authorized", etc.
        private String order_id; // Razorpay Order ID
        private String description;
        private String method; // "card", "netbanking", "upi"
        private String email;
        private String contact;
        @JsonProperty("error_code")
        private String errorCode;
        @JsonProperty("error_description")
        private String errorDescription;
        // ... many more fields from Razorpay PaymentTransactions object ...
    }
}
