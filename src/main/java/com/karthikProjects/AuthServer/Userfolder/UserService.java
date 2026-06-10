package com.karthikProjects.AuthServer.Userfolder;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.karthikProjects.AuthServer.Configuration.ConfigClass.passwordEncoder;

@Primary
@Service
public class UserService implements UserDetailsService {

    @Autowired
    UserRepository repository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        Optional<Users> user = repository.findByUsername(username);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("Given Username is not found in DB");
        }
        return User.builder()
                .username(user.get().getUsername())
                .password(user.get().getPassword())
                .authorities(user.get().getRoles().stream()
                        .map(scope -> new SimpleGrantedAuthority("ROLE_" + scope.name()))
                        .collect(Collectors.toList()))
                .build();
    }

    public void save(Users users) {
        Users user = Users.builder()
                .username(users.getUsername())
                .mail(users.getMail())
                .roles(users.getRoles())
                .password(Objects.requireNonNull(passwordEncoder().encode(users.getPassword())))
                .build();
        repository.save(user);
    }
}
