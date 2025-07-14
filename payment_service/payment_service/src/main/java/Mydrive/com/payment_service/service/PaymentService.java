package Mydrive.com.payment_service.service;

import Mydrive.com.payment_service.config.PaymentPlanConfig;
import Mydrive.com.payment_service.config.PaymentPlanConfig.StoragePlanDetails;
import Mydrive.com.payment_service.dto.PaymentOrderRequest;
import Mydrive.com.payment_service.dto.PaymentOrderResponse;
import Mydrive.com.payment_service.dto.UserPlanUpdateRequestDTO;
import Mydrive.com.payment_service.feign.UserServiceClient;
import Mydrive.com.payment_service.model.PaymentStatus;
import Mydrive.com.payment_service.model.PaymentTransactions;
import Mydrive.com.payment_service.repository.Paymentrepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Component
public class PaymentService {
    @Autowired
    private  Paymentrepository paymentrepository;
    @Autowired
    private UserServiceClient userServiceClient; // Injected Feign client

    @Autowired
    private PaymentPlanConfig paymentPlansConfig;


    @Autowired
    public PaymentService(UserServiceClient userServiceClient){
        this.userServiceClient=userServiceClient;
    }

    // Injected plan configurations

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook_secret}")
    private String razorpayWebhookSecret;

    // Helper to initialize RazorpayClient
    private RazorpayClient getRazorpayClient() throws Exception {
        return new RazorpayClient(razorpayKeyId, razorpayKeySecret);
    }

    @Transactional
    public PaymentOrderResponse createPaymentOrder(Long userId, PaymentOrderRequest request) {
        // 1. Validate and get plan details from config
        StoragePlanDetails planDetails = paymentPlansConfig.getStoragePlans().get(request.getPlanCode());
        if (planDetails == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or unknown plan code: " + request.getPlanCode());
        }

        Object rawAmountPaise = planDetails.getAmountPaise();
        System.out.println("DEBUG: Raw object from getAmountPaise(): " + rawAmountPaise);
        System.out.println("DEBUG: Class of rawAmountPaise: " + rawAmountPaise.getClass().getName());
        Long amountToCharge = Long.valueOf(String.valueOf(rawAmountPaise));
        String currency = paymentPlansConfig.getCurrency();
        System.out.println("here111111");

        try {
            RazorpayClient razorpay = getRazorpayClient();

            // Create Razorpay Order object
            System.out.println("here");
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountToCharge);    // Amount in smallest currency unit (e.g., paisa)
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "receipt_user_" + userId + "_plan_" + request.getPlanCode());
            orderRequest.put("payment_capture", 1); // Auto capture payment after success

            Order order = razorpay.orders.create(orderRequest);

            System.out.println(order);
            // Save the payment order details to your database
            PaymentTransactions payment = new PaymentTransactions();
            payment.setUserId(userId);
            payment.setAmount(amountToCharge);
            payment.setCurrency(currency);
            payment.setRazorpayOrderId(order.get("id")); // Get Razorpay's order ID
            payment.setStatus(PaymentStatus.CREATED); // Initial status
            payment.setPaymentPurpose(request.getPlanCode()); // Store the plan code as purpose
            payment = paymentrepository.save(payment);

            System.out.println(payment);
//            Object rz=order.get("amount");
//            Long err=(Long) rz;
//            System.out.println("hiiii"+err.getClass().getName());

            return PaymentOrderResponse.builder()
                    .razorpayOrderId(order.get("id"))
                    .amount(((Number) order.get("amount")).longValue())
                    .currency(order.get("currency"))
                    .status(order.get("status"))
                    .planCode(request.getPlanCode()) // Return the plan code
                    .paymentId(payment.getId()) // Your internal payment ID
                    .build();


        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create payment order: " + e.getMessage(), e);
        }
    }

    @Transactional
    public boolean handleWebhook(String payload, String signature) {
        try {
            // 1. Verify the webhook signature - CRUCIAL FOR SECURITY
            JSONObject webhookPayload = new JSONObject(payload);

            // Corrected path to the 'entity' object containing 'id', 'order_id', 'amount' etc.
            JSONObject paymentEntity = webhookPayload
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity"); // <--- ADDED .getJSONObject("entity") HERE

            String razorpayPaymentId = paymentEntity.getString("id"); // Now correctly retrieves Payment ID

            // It's good practice to verify signature before processing the payload extensively
            // This ensures you don't spend resources on potentially fraudulent webhooks
            Utils.verifyWebhookSignature(payload, signature, razorpayWebhookSecret);
            System.out.println("Webhook signature verified successfully for payment ID: " + razorpayPaymentId);

            // 2. Extract relevant information and update payment status in database
            String event = webhookPayload.getString("event"); // e.g., "payment.captured", "payment.failed"
            String razorpayOrderId = paymentEntity.getString("order_id"); // Now correctly retrieves Order ID
            String razorpayStatus = paymentEntity.getString("status");
            String errorCode = paymentEntity.optString("error_code", null);
            String errorDescription = paymentEntity.optString("error_description", null);

            // Extract amount from the webhook payload, convert it to Long
            Long webhookAmount = ((Number) paymentEntity.get("amount")).longValue(); // Safe conversion

            Optional<PaymentTransactions> optionalPayment = paymentrepository.findByRazorpayOrderId(razorpayOrderId);

            if (optionalPayment.isEmpty()) {
                System.err.println("PaymentTransactions order not found for Razorpay Order ID: " + razorpayOrderId);
                return false;
            }

            PaymentTransactions payment = optionalPayment.get();

            // Idempotency check: if payment is already successfully processed, just return OK.
            // Also check if the amount matches to prevent potential issues
            if (payment.getStatus() == PaymentStatus.SUCCESS && event.equals("payment.captured") && payment.getAmount().equals(webhookAmount)) {
                System.out.println("PaymentTransactions " + razorpayPaymentId + " already processed as SUCCESS with matching amount. Skipping duplicate webhook.");
                return true;
            }

            // It's also good to check if the amount from the webhook matches the stored amount,
            // although Razorpay's webhooks are usually reliable for this.
            if (!payment.getAmount().equals(webhookAmount)) {
                System.err.println("Amount mismatch for order " + razorpayOrderId + ": Stored=" + payment.getAmount() + ", Webhook=" + webhookAmount);
                // You might want to handle this as a critical error or log it for review
                // For now, we'll proceed but log the warning.
            }

            payment.setRazorpayPaymentId(razorpayPaymentId); // Link the payment ID
            payment.setRazorpaySignature(signature); // Store signature for audit purposes

            switch (event) {
                case "payment.captured":
                    // This is the success event for auto-captured payments
                    payment.setStatus(PaymentStatus.SUCCESS);
                    System.out.println("PaymentTransactions captured successfully for Order ID: " + razorpayOrderId);
                    // 3. Notify other services (e.g., User Service) about successful payment
                    // Pass the necessary details, typically extracted from webhook notes or retrieved from payment
                    notifyUserServiceOnSuccess(payment); // <--- Make sure this method exists and handles required data
                    break;
                case "payment.failed":
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setErrorCode(errorCode);
                    payment.setErrorDescription(errorDescription);
                    System.out.println("PaymentTransactions failed for Order ID: " + razorpayOrderId + " Error: " + errorDescription);
                    break;
                case "payment.authorized":
                    payment.setStatus(PaymentStatus.CAPTURED); // Or a specific AUTHORIZED status
                    System.out.println("PaymentTransactions authorized for Order ID: " + razorpayOrderId);
                    break;
                case "order.paid": // Handle this event specifically as it was received earlier
                    // This event also signifies success for the order.
                    // It contains both payment and order entities.
                    // You might want to process it similarly to payment.captured if your logic is the same.
                    // For now, let's treat it as a success for payment status.
                    payment.setStatus(PaymentStatus.SUCCESS);
                    System.out.println("Order paid event received for Order ID: " + razorpayOrderId);
                    // Re-notify user service if this event might trigger a different flow or if it's a fallback
                    notifyUserServiceOnSuccess(payment);
                    break;
                default:
                    System.out.println("Unhandled Razorpay event: " + event + " for Order ID: " + razorpayOrderId);
                    break;
            }
            paymentrepository.save(payment);
            return true;

        } catch (JSONException e) {
            System.err.println("Error parsing webhook payload (JSON structure issue): " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Error handling Razorpay webhook for payload: " + payload + " | Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private void notifyUserServiceOnSuccess(PaymentTransactions payment) {
        try {
            String planCode = payment.getPaymentPurpose(); // This is where we stored the plan code
            PaymentPlanConfig.StoragePlanDetails planDetails = paymentPlansConfig.getStoragePlans().get(planCode);


            if (planDetails == null) {
                System.err.println("Error: No plan details found in config for payment purpose/planCode: " + planCode + ". User Service not notified for payment ID: " + payment.getId());
                return;
            }

            // Create update request for User Service
            UserPlanUpdateRequestDTO userPlanUpdate = new UserPlanUpdateRequestDTO();
            // Set the new plan name based on the purchased plan
            userPlanUpdate.setNewPlan(planDetails.getPlanname()); // <--- IMPORTANT: Set the new plan name
            // Convert GB to Bytes for storage update
            userPlanUpdate.setAddExtrabytes(Long.valueOf(planDetails.getStorageGb() * 1024L * 1024L * 1024L)); // <--- Set storage in Bytes

            // Call the User Service Feign client

            System.out.println(payment.getUserId());
            System.out.println(userPlanUpdate);
            userServiceClient.updateUserPlan(payment.getUserId(), userPlanUpdate);
            System.out.println("User Service notified for user " + payment.getUserId() + " payment success. New Plan: " + planDetails.getPlanname() + ", Added " + planDetails.getStorageGb() + "GB.");

        } catch (Exception e) {
            System.err.println("Failed to notify User Service for payment success (payment ID: " + payment.getId() + "): " + e.getMessage());
            e.printStackTrace();
            // Implement robust retry mechanism here (e.g., Message Queue, Spring Retry)
            // to ensure the user's plan is updated even if the User Service is temporarily down.
        }
    }
    public PaymentTransactions getPaymentDetails(Long paymentId, Long userId) {
        return paymentrepository.findById(paymentId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PaymentTransactions not found or not accessible."));
    }

    @Transactional
    public void initiateRefund(Long paymentId, Long userId, Long refundAmount) {
        // 1. Fetch payment from DB and validate
        PaymentTransactions payment = getPaymentDetails(paymentId, userId);
        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PaymentTransactions is not in a refundable state (must be SUCCESS or CAPTURED).");
        }
        if (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Original Razorpay PaymentTransactions ID is missing for refund.");
        }
        if (refundAmount == null || refundAmount <= 0 || refundAmount > payment.getAmount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid refund amount. Must be positive and not exceed original amount.");
        }

        try {
            RazorpayClient razorpay = getRazorpayClient();
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", refundAmount);

            com.razorpay.Refund refund = razorpay.payments.refund(payment.getRazorpayPaymentId(), refundRequest);

            // 2. Update payment status in DB
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setErrorCode(null);
            payment.setErrorDescription(null);
            paymentrepository.save(payment);
            System.out.println("Refund initiated for payment " + paymentId + ". Razorpay Refund ID: " + refund.get("id"));

            // 3. Notify User Service to reverse the added storage and potentially revert plan
            notifyUserServiceOnRefund(payment, refundAmount);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to initiate refund: " + e.getMessage(), e);
        }
    }

    private void notifyUserServiceOnRefund(PaymentTransactions payment, Long refundedAmount) {
        try {
            String planCode = payment.getPaymentPurpose();
            PaymentPlanConfig.StoragePlanDetails planDetails = paymentPlansConfig.getStoragePlans().get(planCode);

            if (planDetails == null) {
                System.err.println("Error: No plan details found for planCode: " + planCode + " during refund processing. User Service not notified.");
                return;
            }

            UserPlanUpdateRequestDTO userPlanUpdate = new UserPlanUpdateRequestDTO();
            userPlanUpdate.setNewPlan(null); // Default: don't change plan on partial refund

            // Calculate proportionate storage to deduct
            Long storageToDeductBytes = 0L;
            if (refundedAmount.equals(payment.getAmount())) {
                // Full refund: deduct full storage and revert to FREE plan
                storageToDeductBytes = Long.valueOf(-planDetails.getStorageGb() * 1024L * 1024L * 1024L);
                userPlanUpdate.setNewPlan("Free"); // Revert to "Free" plan on full refund
            } else {
                // Partial refund: deduct proportionally
                storageToDeductBytes = (long) Math.round((double) refundedAmount / payment.getAmount() * planDetails.getStorageGb() * 1024 * 1024 * 1024);
                storageToDeductBytes = -storageToDeductBytes; // Make it negative for deduction
            }
            userPlanUpdate.setAddExtrabytes(storageToDeductBytes);

            userServiceClient.updateUserPlan(payment.getUserId(), userPlanUpdate);
            System.out.println("User Service notified for user " + payment.getUserId() + " refund. Deducted " + (storageToDeductBytes / (1024 * 1024 * 1024)) + "GB. New Plan: " + userPlanUpdate.getNewPlan());
        } catch (Exception e) {
            System.err.println("Failed to notify User Service for refund (payment ID: " + payment.getId() + "): " + e.getMessage());
            e.printStackTrace();
            // Implement retry/DLQ here for compensation
        }
    }
}
