package com.karthikProjects.AuthServer.UserService;

import com.karthikProjects.AuthServer.Userfolder.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

@SpringBootTest
class UserControllerTest {
    @Autowired
    UserController userController;
    @Autowired
    UserService userService;

    @Autowired
            UserRepository userRepository;

    Users users;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        ArrayList <Scopes> scopes = new ArrayList<>();
        scopes.add(Scopes.ADMIN);
        users = Users.builder().id(1).username("Karthik")
                .mail("test@test.com").password("Test").roles(scopes).build();
    }

    @Test
    @Transactional
    void createUsers() {
        userController.CreateUsers(users);
        Assertions.assertEquals("Karthik", userService.loadUserByUsername("Karthik").getUsername());
    }


}