package Mydrive.com.file_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageUpdateRequest {

    @NotNull
    private Long userid;

    @NotNull
    private Long bytesChange;

    private Long extraStoragebytes;
}
