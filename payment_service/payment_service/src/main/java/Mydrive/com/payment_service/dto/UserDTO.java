package Mydrive.com.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long userid;

    private String username;

    private String email;

    private Long allocatedstoragebytes;

    private Long usedstoragebytes;

    private String currentplan;
}
