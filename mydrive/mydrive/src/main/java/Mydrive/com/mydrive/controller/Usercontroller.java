package Mydrive.com.mydrive.controller;

import Mydrive.com.mydrive.DTOS.*;
import Mydrive.com.mydrive.service.Userservice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/users")
public class Usercontroller {

    @Autowired
    private Userservice userservice;

    @GetMapping("check")
    public String checkresponse(){
        return "Hello";
    }

    @PostMapping("register")
    public ResponseEntity<?> Registeruser(@RequestBody RegisterDTO details){
        RegisterResponseDTO regdetails= userservice.registerUser(details);

        return new ResponseEntity<>(regdetails, HttpStatus.CREATED);
    }

    @PostMapping("login")
    public ResponseEntity<?> LoginAuthenticateandgetToken(@RequestBody LoginRequestDTO requestDTO){
        LoginResponseDTO responseDTO=userservice.findbyUsername(requestDTO);

        return new ResponseEntity<>(responseDTO,HttpStatus.OK);
    }

    @GetMapping("profile")
    public ResponseEntity<?> getuserprofile(@RequestHeader("Authorization") String authheader){
        UserDTO response=userservice.getprofile(authheader);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @PutMapping("profile/password")
    public ResponseEntity<?> updatepassword(@RequestHeader("Authorization") String authheader,
                                            @RequestBody Passwordupdaterequest passwordupdaterequest){
        String response=userservice.updateuserpassword(authheader,passwordupdaterequest);

        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @PutMapping("internal/users/{id}/used_storage")
    public ResponseEntity<?> updatedusedstorage(@PathVariable Long id,
                                                @RequestBody StorageUpdateDTO request){
        UserDTO updateusedstorage=userservice.updatestorageused(id,request);
        return new ResponseEntity<>(updateusedstorage,HttpStatus.OK);
    }

    @PutMapping("/internal/users/{userId}/update-plan")
    public ResponseEntity<UserDTO> updateUserPlan(@PathVariable Long userId,
                                                       @RequestBody UserPlanUpdateRequestDTO request) {
        // Implement internal authentication/authorization if needed
        UserDTO updatedUser = userservice.updateUserPlanandStorage(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/internal/users/{userId}/storage-info")
    public ResponseEntity<UserDTO> getStorageInfo(@PathVariable Long userId) {
        // Implement internal authentication/authorization if needed
        return ResponseEntity.ok(userservice.getUserProfile(userId));
    }
}
