package Mydrive.com.file_service.sharedDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageUpdateDTO {
    // This field must match the one expected by updateUsedStorage in UserService (e.g., bytesChange)
    private Long bytesChange;
}
