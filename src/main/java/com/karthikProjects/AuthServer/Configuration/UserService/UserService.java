package com.karthikProjects.AuthServer.Configuration.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Users> user=repository.findByUsername(username);
        if(user.isEmpty()){throw new UsernameNotFoundException("Given Username is not found in DB");}
        return User.builder().username(user.get().getUsername())
                .password(user.get().getPassword())
                .roles(String.valueOf(user.get().getScopes()))
                .build();
    }
}
