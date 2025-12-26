package com.karthikProjects.AuthServer.Configuration.UserService;


import com.karthikProjects.AuthServer.Configuration.Scopes;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@AllArgsConstructor
@Builder
@Entity
@Table(name = "Users_for_authentication")
@Getter
@Setter
@NoArgsConstructor
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @NonNull
    private String username;

    @NonNull
    private String password;

    @NonNull
    @Pattern(regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]{1,50}+@[a-zA-Z0-9.-]+\\.[A-Za-z]{2,3}$",message = "Email should start and should consist 1,12 alpha-numeric characters(Can also Contain special characters) have @ in between and should end with domain")
    private String mail;

    @NonNull
    private ArrayList<Scopes> roles;

    public @NonNull String getUsername() {
        return this.username;
    }

    public @NonNull String getPassword() {
        return this.password;
    }

    public @NonNull List<Scopes> getScopes() {
        return this.roles;
    }
}
