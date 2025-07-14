package Mydrive.com.mydrive.service;

import Mydrive.com.mydrive.DTOS.*;

public interface Userservice {
    RegisterResponseDTO registerUser(RegisterDTO details);

    LoginResponseDTO findbyUsername(LoginRequestDTO requestDTO);

    UserDTO getprofile(String authheader);


    String updateuserpassword(String authheader, Passwordupdaterequest passwordupdaterequest);

    UserDTO updatestorageused(Long id, StorageUpdateDTO request);

    UserDTO updateUserPlanandStorage(Long id, UserPlanUpdateRequestDTO request);

    UserDTO getUserProfile(Long id);
}
