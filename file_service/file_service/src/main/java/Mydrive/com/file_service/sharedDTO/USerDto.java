package Mydrive.com.file_service.sharedDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class USerDto {
    private Long userid;

    private String username;

    private String email;

    private Long allocatedstoragebytes;

    private Long usedstoragebytes;
}
