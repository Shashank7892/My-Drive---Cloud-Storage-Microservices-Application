package Mydrive.com.payment_service.repository;

import Mydrive.com.payment_service.model.PaymentTransactions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Paymentrepository extends JpaRepository<PaymentTransactions,Long> {
    Optional<PaymentTransactions> findByRazorpayOrderId(String razorpayOrderId);
    Optional<PaymentTransactions> findByRazorpayPaymentId(String razorpayPaymentId);
}
