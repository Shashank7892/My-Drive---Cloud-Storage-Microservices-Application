package Mydrive.com.file_service.controller;

import Mydrive.com.file_service.dto.FileMetaDataDTO;
import Mydrive.com.file_service.dto.Fileresponsedto;
import Mydrive.com.file_service.feignClient.UserServiceClent;
import Mydrive.com.file_service.service.Fileservice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("api/files")
public class Filecontroller {

    @Autowired
    private Fileservice fileservice;


    private Long getUserId(Authentication authentication){
        System.out.println("Authentication object: " + authentication);

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated.");
        }

        // Correctly extract userId from the Jwt principal
        if (authentication.getPrincipal() instanceof Jwt jwtPrincipal) {
            Long userIdInteger = jwtPrincipal.getClaim("userid");
            System.out.println("Extracted userId from JWT: " + userIdInteger);

            if (userIdInteger == null) {
                // If 'userid' claim is missing, it's a misconfigured token or a security issue
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User ID claim ('userid') not found in JWT.");
            }
            return userIdInteger.longValue(); // Convert to Long
        } else {
            // This case should ideally not happen with proper JWT authentication setup
            System.err.println("Authentication principal is not a JWT: " + authentication.getPrincipal().getClass().getName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication principal is not a JWT. Cannot extract user ID.");
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Fileresponsedto> uploadFile(@RequestParam("file") MultipartFile file
                                                      ,Authentication authentication) {
        System.out.println("hiiii"+authentication);
        Long userId =getUserId(authentication); // Changed userId type to Long
        Fileresponsedto response = fileservice.uploadFile(userId, file);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/getfiles")
    public ResponseEntity<List<FileMetaDataDTO>> listUserFiles(Authentication authentication) {
        Long userId = getUserId(authentication); // Changed userId type to Long
        List<FileMetaDataDTO> files = fileservice.getUserfiles(userId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{fileId}/download-url")
    public ResponseEntity<String> getDownloadUrl(@PathVariable Long fileId,Authentication authentication) { // Changed fileId type to Long
        Long userId =getUserId(authentication); // Changed userId type to Long
        String downloadUrl = fileservice.getFilesDownloadURL(userId, fileId);
        return ResponseEntity.ok(downloadUrl);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<String> deleteFile(@PathVariable Long fileId, Authentication authentication) { // Changed fileId type to Long
        Long userId = getUserId(authentication); // Changed userId type to Long
        fileservice.deleteFile(userId, fileId);
        return ResponseEntity.ok("File deleted successfully.");
    }

    @GetMapping("/{fileId}/metadata")
    public ResponseEntity<FileMetaDataDTO> getFileMetadata(@PathVariable Long fileId , Authentication authentication) { // Changed fileId type to Long
        Long userId = getUserId(authentication); // Changed userId type to Long
        FileMetaDataDTO metadata = fileservice.getFileMetaData(userId, fileId);
        return ResponseEntity.ok(metadata);
    }
}
