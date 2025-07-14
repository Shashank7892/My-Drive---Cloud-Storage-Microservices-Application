package Mydrive.com.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for updating user plan, tailor this to your User Service's actual API
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPlanUpdateRequestDTO {
    private String newPlan; // e.g., "PREMIUM", "PRO", "FREE" - or null if only adding storage
    private Long addExtrabytes; // Positive for adding, negative for removing
    // Add other fields relevant to plan changes if needed, e.g., plan_duration, etc.
}
