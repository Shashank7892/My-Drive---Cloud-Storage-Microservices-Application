package Mydrive.com.mydrive.DTOS;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageUpdateDTO {

    /**
     * The amount of extra storage in bytes to add to the user's current allocated storage.
     * This value should always be positive, as it represents an increase.
     * (e.g., 2GB = 2 * 1024 * 1024 * 1024 bytes)
     */

    @NotNull(message = "field cannot be null")
    @Positive(message = "Extra Storage Bytes must be positive")
    private Long bytesChange;

}
