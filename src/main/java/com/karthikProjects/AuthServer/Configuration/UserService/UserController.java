package com.karthikProjects.AuthServer.Configuration.UserService;

import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class UserController {

    @Autowired
    private UserService userService;
    @PostMapping(value = "/saveuser",consumes = "application/json")
    void CreateUsers(@RequestBody Users users){
        userService.save(users);
    }
}
