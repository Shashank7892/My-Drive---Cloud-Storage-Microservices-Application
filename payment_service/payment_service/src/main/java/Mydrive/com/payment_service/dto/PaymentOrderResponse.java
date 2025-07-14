package Mydrive.com.payment_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentOrderResponse {
    private String razorpayOrderId; // The order ID from Razorpay
    private Long amount; // The actual amount from the chosen plan
    private String currency;
    private String status; // Status of the order (e.g., "created")
    private String planCode; // The chosen plan code
    private Long paymentId; // Your internal payment ID
}
