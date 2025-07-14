package Mydrive.com.payment_service.dto;

import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;

@Data
public class PaymentOrderRequest {
    private String planCode; // e.g., "storage_2gb_topup", "storage_5gb_topup", etc.
    // The amount and currency will be derived on the backend based on this planCode.
}

