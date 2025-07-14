package Mydrive.com.payment_service.model;

public enum PaymentStatus {
    CREATED,    // Razorpay order created
    PENDING,    // PaymentTransactions initiated but not yet successful/failed
    CAPTURED,   // PaymentTransactions successfully captured by Razorpay
    SUCCESS,    // Our internal success state (after webhook processing and other service updates)
    FAILED,
    REFUNDED    // Full or partial refund processed
}
