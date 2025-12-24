package com.karthikProjects.AuthServer;

import com.karthikProjects.AuthServer.Configuration.Scopes;
import com.karthikProjects.AuthServer.Configuration.UserService.UserController;
import com.karthikProjects.AuthServer.Configuration.UserService.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

@SpringBootTest
class AuthServerApplicationTests {

    @Autowired
    UserController controller;

	@Test
	void contextLoads() {
	}
    @Test
    @DisplayName("Save user Test")
    void saveUserTest(){
        ArrayList<Scopes> roles = new ArrayList<String>();
        roles.add("admin")
        Users testUser=Users.builder().id(1).roles(roles);
    }

}
