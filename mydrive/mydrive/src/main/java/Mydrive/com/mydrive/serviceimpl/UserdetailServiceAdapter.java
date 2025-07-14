package Mydrive.com.mydrive.serviceimpl;

import Mydrive.com.mydrive.model.Users;
import Mydrive.com.mydrive.repository.Userrepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;


@Component
@RequiredArgsConstructor
public class UserdetailServiceAdapter implements UserDetailsService {

    private final Userrepository userrepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users users=userrepository.findByUsername(username).orElseThrow(()->new UsernameNotFoundException("User not found"+username));

        return new User(users.getUsername(),users.getPassword(),Collections.emptyList());//Collections.emptyList() role based access// No roles for now, or fetch from user.getRole()
    }
}
