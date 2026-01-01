package com.karthikProjects.AuthServer.Configuration.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    private UserService userService;
    @PostMapping(value = "/saveuser",consumes = "application/json")
    void CreateUsers(@RequestBody Users users){
        userService.save(users);
    }
}
