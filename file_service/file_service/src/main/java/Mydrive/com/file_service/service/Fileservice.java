package Mydrive.com.file_service.service;

import Mydrive.com.file_service.dto.FileMetaDataDTO;
import Mydrive.com.file_service.dto.Fileresponsedto;
import Mydrive.com.file_service.dto.StorageUpdateRequest;
import Mydrive.com.file_service.dto.UserStorageInfo;
import Mydrive.com.file_service.feignClient.UserServiceClent;
import Mydrive.com.file_service.model.Files;
import Mydrive.com.file_service.repository.Filerepository;
import Mydrive.com.file_service.sharedDTO.StorageUpdateDTO;
import Mydrive.com.file_service.sharedDTO.USerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Component
@RequiredArgsConstructor
public class Fileservice {

    @Autowired
    private Filerepository filerepository;

    @Autowired
    private FileStorageservice fileStorageservice;

    @Autowired
    private UserServiceClent userServiceClent;

    @Value("${file.max-upload-size-bytes}")
    private Long maxIndividualFileSize;

    @Autowired
    public Fileservice(UserServiceClent userServiceClent){
        this.userServiceClent=userServiceClent;
    }


    public Fileresponsedto uploadFile(Long userid, MultipartFile file){
        if(file.isEmpty()){
            throw new IllegalArgumentException("cannot upload empty file..");
        }
        if(file.getSize()>maxIndividualFileSize){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"file size exceeds the allowed limit of "+(maxIndividualFileSize/(1024*1024))+"MB.");

        }

        USerDto userStorageInfo=userServiceClent.getStorageInfo(userid);
        System.out.println(userStorageInfo.getUserid());
        System.out.println(userStorageInfo.getAllocatedstoragebytes());
        System.out.println(userStorageInfo.getUsedstoragebytes());
        System.out.println(userStorageInfo.getUsername());
        Long allocatedStorageBytes= userStorageInfo.getAllocatedstoragebytes();
        Long usedStorageBytes = userStorageInfo.getUsedstoragebytes();
        Long availableStorageBytes = allocatedStorageBytes - usedStorageBytes;

        if(file.getSize() > availableStorageBytes){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient Storage space. Available: " + (availableStorageBytes / (1024 * 1024)) + "MB.");
        }
        if(filerepository.existsByUseridAndFilenameAndIsDeletedFalse(userid, file.getOriginalFilename())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "File with the same name already exists for this user.");
        }

        try {
            String objectStoragePath = fileStorageservice.uploadFile(userid, file);

            Files newfiles = new Files();
            newfiles.setUserid(userid);
            newfiles.setFilename(file.getOriginalFilename());
            newfiles.setFileExtension(getFileExtension(file.getOriginalFilename()));
            newfiles.setFileSize(file.getSize());
            newfiles.setMimeType(file.getContentType());
            newfiles.setObjectStoragepath(objectStoragePath);
            newfiles = filerepository.save(newfiles);

            // 4. Update user's used storage in User Service (increment by file size)
            // This replaces the commented-out call
            userServiceClent.updateUsedStorage(userid, new StorageUpdateDTO(file.getSize()));

            return new Fileresponsedto(
                    newfiles.getId(),
                    newfiles.getFilename(),
                    newfiles.getFileSize(),
                    newfiles.getMimeType(),
                    newfiles.getUploadedAt(),
                    "File uploaded successfully."
            );
        } catch (Exception e) {
            e.printStackTrace(); // Consider using a logger instead of printStackTrace
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload the file: " + e.getMessage(), e);
        }
    }
    public List<FileMetaDataDTO> getUserfiles(Long userid){
            List<Files> files=filerepository.findByUseridAndIsDeletedFalse(userid);
            System.out.println(files);
            return files.stream()
                    .map(this::convertToMetaDataDto)
                    .collect(Collectors.toList());
    }
    public String getFilesDownloadURL(Long userid,Long id){
        Files files=filerepository.findByIdAndUseridAndIsDeletedFalse(id,userid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"File Nt found OR Assessible"));

        try{
            return fileStorageservice.getPresignedDownloadUrl(files.getObjectStoragepath(),3600);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Failed to Generate Download URL."+e.getMessage(),e);
        }
    }

    //Alternative: Direct streaming download (less preferred for large files through API Gateway)

    public InputStreamResource downloadFileStream(Long userid,Long id){
        Files files=filerepository.findByIdAndUseridAndIsDeletedFalse(id,userid).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"File not found or not accessible."));
        try{
            InputStream inputStream=fileStorageservice.downloadFile(files.getObjectStoragepath());
            return new InputStreamResource(inputStream);
        }
        catch (Exception e){
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Failed to download file: " + e.getMessage(), e);
        }
    }

    public void deleteFile(Long userid,Long id){
    Files files=filerepository.findByIdAndUseridAndIsDeletedFalse(id,userid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"File not found or not accessible."));
    try{
        // 1. Delete from Object Storage from aws
        fileStorageservice.deleteFile(files.getObjectStoragepath());

        // 2. Update metadata in File Service database (soft delete)
        files.setIsDeleted(true);
        files.setDeletedAt(LocalDateTime.now());
        filerepository.save(files);
        // Save the soft-deleted status
        // 3. Update user's used storage in User Service (decrement)
        userServiceClent.updateUsedStorage(userid,new StorageUpdateDTO(-files.getFileSize()));

    } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Failed to delete file: " + e.getMessage(), e);
    }
    }


    public FileMetaDataDTO getFileMetaData(Long userid,Long id){
        Files files=filerepository.findByIdAndUseridAndIsDeletedFalse(id,userid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"File not found or not accessible."));
        return convertToMetaDataDto(files);
    }

    private FileMetaDataDTO convertToMetaDataDto(Files files){

        return new FileMetaDataDTO(
                files.getId(),
                files.getFilename(),
                files.getFileExtension(),
                files.getFileSize(),
                files.getMimeType(),
                files.getUploadedAt(),
                files.getLastmodifiedAt()
        );
    }

    private String getFileExtension(String filename){
        int lastDotIndex=filename.lastIndexOf('.');
        if(lastDotIndex>0 && lastDotIndex<filename.length()-1){
            return filename.substring(lastDotIndex+1);
        }
        return "";
    }
}
