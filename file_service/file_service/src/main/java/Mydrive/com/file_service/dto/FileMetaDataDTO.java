package Mydrive.com.file_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMetaDataDTO {

    private Long id;

    private String filename;

    private String fileExtension;

    private Long fileSize;

    private String mimeType;

    private LocalDateTime uploadedAt;

    private LocalDateTime lastmodifiedAt;
}
