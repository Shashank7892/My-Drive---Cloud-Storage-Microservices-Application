package Mydrive.com.file_service.feignClient;


import Mydrive.com.file_service.sharedDTO.AddStorageDTO;
import Mydrive.com.file_service.sharedDTO.StorageUpdateDTO;
import Mydrive.com.file_service.sharedDTO.USerDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", configuration = UserInternalFeignClientConfig.class)
public interface UserServiceClent {

    @PutMapping("/api/users/internal/users/{id}/used_storage")
    USerDto updateUsedStorage(@PathVariable("id") Long id, @RequestBody StorageUpdateDTO request);

    @GetMapping("/api/users/internal/users/{userId}/storage-info")
    USerDto getStorageInfo(@PathVariable("userId") Long userId);

}
