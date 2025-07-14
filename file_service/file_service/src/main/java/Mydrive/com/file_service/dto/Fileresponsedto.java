package Mydrive.com.file_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Fileresponsedto {

    private Long id;

    private String filename;

    private Long filesize;

    private String mimeType;

    private LocalDateTime uploadedAt;

    private String message;
}
