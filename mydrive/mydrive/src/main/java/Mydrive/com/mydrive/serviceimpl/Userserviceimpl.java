package Mydrive.com.mydrive.serviceimpl;

import Mydrive.com.mydrive.DTOS.*;
import Mydrive.com.mydrive.customexceptions.UserAlreadyExistsException;
import Mydrive.com.mydrive.customexceptions.UserNotFoundException;
import Mydrive.com.mydrive.jwt.JwtService;
import Mydrive.com.mydrive.model.Users;
import Mydrive.com.mydrive.repository.Userrepository;
import Mydrive.com.mydrive.service.Userservice;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Userserviceimpl implements Userservice {

    @Autowired
    private Userrepository userrepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired

    private UserdetailServiceAdapter userdetailServiceAdapter;

    @Autowired
    private  JwtService jwtService;
    @Override
    @Transactional
    public RegisterResponseDTO registerUser(RegisterDTO details) {
        if(userrepository.existsByUsername(details.getUsername())){
            throw new UserAlreadyExistsException("Username Already Exists:"+ details.getUsername());
        }
        if(userrepository.existsByEmail(details.getEmail())){
            throw new UserAlreadyExistsException("Email already Exists:" + details.getEmail());
        }

        Users users=new Users();
        users.setUsername(details.getUsername());
        users.setEmail(details.getEmail());
        users.setPassword(passwordEncoder.encode(details.getPassword()));
        //default execuete of prepersist
        userrepository.save(users);
        RegisterResponseDTO registerResponseDTO=new RegisterResponseDTO();
        registerResponseDTO.setEmail(users.getEmail());
        registerResponseDTO.setUsername(users.getUsername());
        registerResponseDTO.setMessage("User Registered successfully");
        return registerResponseDTO;
    }

    @Override
    public LoginResponseDTO findbyUsername(LoginRequestDTO requestDTO) {
        LoginResponseDTO responseDTO=new LoginResponseDTO();
        Authentication authentication= authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestDTO.getUsername(),requestDTO.getPassword()));
        System.out.println("hereerre");
                if(authentication.isAuthenticated()){
                    UserDetails userDetails=userdetailServiceAdapter.loadUserByUsername(requestDTO.getUsername());
                    Users users=userrepository.findByUsername(userDetails.getUsername()).orElseThrow(()->new UserNotFoundException("user bot found"));
                    String token=jwtService.generatetoken(requestDTO.getUsername(),users.getId());
                    responseDTO.setToken(token);
                    responseDTO.setUserid(users.getId());
                    return responseDTO;
                }
                else{
                    System.out.println("hereee....");
                    return null;
                }
    }

    @Override
    public UserDTO getprofile(String authheader) {
        System.out.println("hjellllo"+authheader);
        Users user=getusername(authheader);
        UserDTO userDTO=new UserDTO();
        userDTO.setUserid(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setUsername(user.getUsername());
        userDTO.setAllocatedstoragebytes(user.getAllallocatedstorageBytes());
        userDTO.setUsedstoragebytes(user.getUsedstorageBytes());
        userDTO.setCurrentplan(user.getCurrentPlan());
        return userDTO;

    }

    @Override
    @Transactional
    public String updateuserpassword(String authheader, Passwordupdaterequest passwordupdaterequest) {
        Users user=getusername(authheader);
        String oldpassword=passwordupdaterequest.getOldpassword();
        if(!passwordEncoder.matches(oldpassword,user.getPassword())){
            throw new IllegalArgumentException("current password doesnot match the old password");
        }
        user.setPassword(passwordEncoder.encode(passwordupdaterequest.getNewpassword()));
        userrepository.save(user);
        return "password changed successfully";
    }

    @Override
    public UserDTO updatestorageused(Long id, StorageUpdateDTO request) {
        Users users=userrepository.findById(id).orElseThrow(()->new UserNotFoundException("Not found"));
        long newusedstorage=users.getUsedstorageBytes()+request.getBytesChange();
        if(newusedstorage<0){
            newusedstorage=0;
        }
        if(newusedstorage>users.getAllallocatedstorageBytes()){
            throw new IllegalArgumentException("Attemped to use more space than the allocated storage");
        }
        users.setUsedstorageBytes(newusedstorage);
        userrepository.save(users);
        return convertToDto(users);
    }

    @Override
    @Transactional
    public UserDTO updateUserPlanandStorage(Long id, UserPlanUpdateRequestDTO request) {
        Users users=userrepository.findById(id).orElseThrow(()->new UserNotFoundException("User not found with ID: " + id));

        // Validate that at least one update was requested
        if ((request.getAddExtrabytes() == null || request.getAddExtrabytes() <= 0) && (request.getNewplan() == null || request.getNewplan().isEmpty())) {
            throw new IllegalArgumentException("No valid plan or storage update requested for user: " + id);
        }
        if(request.getAddExtrabytes()!=null && request.getAddExtrabytes()>0){
            Long currentallocatedbytes=users.getAllallocatedstorageBytes()!=null ? users.getAllallocatedstorageBytes() : 0L;
            users.setAllallocatedstorageBytes(currentallocatedbytes+request.getAddExtrabytes());
        }
        if(request.getNewplan()!=null && !request.getNewplan().isEmpty()){
            System.out.println(request.getNewplan());
            users.setCurrentPlan(request.getNewplan());
        }
        userrepository.save(users);
        return convertToDto(users);
    }

    @Override
    public UserDTO getUserProfile(Long id) {
        Users users=userrepository.findById(id).orElseThrow(()->new UserNotFoundException("User not found with ID: " + id));
        return convertToDto(users);
    }


    private UserDTO convertToDto(Users users) {
        UserDTO dto = new UserDTO();
        dto.setUserid(users.getId());
        dto.setUsername(users.getUsername());
        dto.setEmail(users.getEmail());
        dto.setAllocatedstoragebytes(users.getAllallocatedstorageBytes());
        dto.setUsedstoragebytes(users.getUsedstorageBytes());
        dto.setCurrentplan(users.getCurrentPlan());
        return dto;
    }

    private Users getusername(String authheader){
        System.out.println(authheader);
        String token=authheader.substring(7);
        String username= jwtService.extractUsername(token);
        Users user=userrepository.findByUsername(username).orElseThrow(()->new UsernameNotFoundException("user not exists"+username));
        return user;
    }

}
