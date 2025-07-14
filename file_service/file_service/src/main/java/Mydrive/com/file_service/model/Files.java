package Mydrive.com.file_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "files")
@AllArgsConstructor
@NoArgsConstructor
public class Files {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "user_id",nullable = false)
    private Long userid;

    @Column(nullable = false)
    private String filename;

    @Column(name = "file_extension",length = 10)
    private String fileExtension;

    @Column(name = "file_size_bytes",nullable = false)
    private Long fileSize;

    @Column(name = "mime_type",length = 100)
    private String mimeType;

    @Column(name = "object_storage_path",nullable = false,unique = true)
    private String ObjectStoragepath;

    @Column(name = "uploaded_at",nullable = false,updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "last_modified_at",nullable = false)
    private LocalDateTime lastmodifiedAt;

    @Column(name="is_Deleted",nullable = false)
    private Boolean isDeleted;

    @Column(name = "deleted_At")
    private LocalDateTime deletedAt;

    @PrePersist
    private void onCreate(){
        this.uploadedAt=LocalDateTime.now();
        this.lastmodifiedAt=LocalDateTime.now();
        if(this.isDeleted==null){
            this.isDeleted=false;
        }
    }
    @PreUpdate
    private void onUpdate(){
        this.lastmodifiedAt=LocalDateTime.now();
    }
}
