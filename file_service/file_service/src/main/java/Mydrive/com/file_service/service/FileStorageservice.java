package Mydrive.com.file_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

@Component
public class FileStorageservice {

    @Autowired
    private S3Client s3Client;  // Inject the S3Client

    @Autowired
    private S3Presigner s3Presigner; // Inject the S3Presigner for URLs

    @Value("${aws.s3.bucketname}")
    private String bucketname;


    @Value("${file.max-upload-size-bytes}")
    private long maxIndividualFileSize;

    public String uploadFile(Long userid, MultipartFile file)throws Exception{
        if(file.getSize()>maxIndividualFileSize){
            throw new IllegalArgumentException("file size exceeds allowed limit");
        }
        String objectkey=userid.toString()+"/"+file.getOriginalFilename();


        PutObjectRequest putObjectRequest=PutObjectRequest.builder()
                .bucket(bucketname)
                .key(objectkey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try(InputStream is=file.getInputStream()){
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(is,file.getSize()));
        }
        return objectkey;
    }

    public InputStream downloadFile(String objectKey) throws Exception{
        GetObjectRequest getObjectRequest=GetObjectRequest.builder()
                .bucket(bucketname)
                .key(objectKey)
                .build();

        try{
            return s3Client.getObject(getObjectRequest);
        }
        catch (NoSuchKeyException e){
            throw new Exception("File Not found in s3:"+objectKey,e);
        }
        catch (Exception e){
            throw new Exception("Failed to download file from S3:"+objectKey,e);
        }
    }

    public void deleteFile(String objectKey)throws Exception{
        DeleteObjectRequest deleteObjectRequest=DeleteObjectRequest.builder()
                .bucket(bucketname)
                .key(objectKey)
                .build();
        try{
            s3Client.deleteObject(deleteObjectRequest);
        }
        catch (Exception e){
            throw new Exception("Failed to delete file from S3:"+objectKey,e);
        }
    }

    public String getPresignedDownloadUrl(String objectKey,int expirySeconds) throws Exception{
        GetObjectRequest getObjectRequest=GetObjectRequest.builder()
                .bucket(bucketname)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest=GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirySeconds))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest=s3Presigner.presignGetObject(presignRequest);
        URL url=presignedRequest.url();
        return url.toString();
    }
}
