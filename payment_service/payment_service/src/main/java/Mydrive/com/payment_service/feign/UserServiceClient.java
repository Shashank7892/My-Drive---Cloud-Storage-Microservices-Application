package Mydrive.com.payment_service.feign;

import Mydrive.com.payment_service.dto.UserDTO;
import Mydrive.com.payment_service.dto.UserPlanUpdateRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service",configuration = UserInternalFeignClientConfig.class)
public interface UserServiceClient {
    @PutMapping("/api/users/internal/users/{userId}/update-plan")
    ResponseEntity<UserDTO> updateUserPlan(@PathVariable("userId") Long userId, @RequestBody UserPlanUpdateRequestDTO request);

    // You might also add methods like getUserProfile, getUserEmail if needed by PaymentTransactions Service.
    // (As discussed in previous responses)
}

