package com.karthikProjects.AuthServer.Configuration.UserService;

import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @PostMapping(value = "/saveuser",consumes = "Application/JSON")
    void CreateUsers(@RequestBody Users users){
        userRepository.save(users);
    }
}
