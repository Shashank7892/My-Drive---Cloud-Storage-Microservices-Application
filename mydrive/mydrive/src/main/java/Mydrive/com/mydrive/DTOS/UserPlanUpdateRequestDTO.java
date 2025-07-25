package Mydrive.com.mydrive.DTOS;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPlanUpdateRequestDTO {

    private String newplan;

    private Long addExtrabytes;
}
