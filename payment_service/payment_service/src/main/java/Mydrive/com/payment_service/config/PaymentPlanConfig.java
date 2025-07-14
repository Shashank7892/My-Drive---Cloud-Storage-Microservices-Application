package Mydrive.com.payment_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "payment")
@Data
public class PaymentPlanConfig {
    private String currency;

    private Map<String,StoragePlanDetails> storagePlans;

    @Data
    public static class StoragePlanDetails{
        private Long amountPaise; // Price in smallest currency unit (e.g., paise for INR)
        private Integer storageGb;
        private String planname;// Storage in GB to add
    }
}
