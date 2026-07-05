package com.karthikProjects.AuthServer;
import com.karthikProjects.AuthServer.Userfolder.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.AssertionErrors;

import java.util.ArrayList;

@TestPropertySource("/application.properties")
@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
class AuthServerApplicationTests {

	@Autowired
	UserController userController;

	@Autowired
	UserRepository userRepository;


	@Test
	void contextLoads() {
	}

	@Test
	void testSaveUser() {
		ArrayList<Scopes> testScopes =new ArrayList<>();
		testScopes.add(Scopes.ADMIN);
		Users testUser=Users.builder().username("testUser").password("Karthik").mail("Hello@Gmail.com").roles(testScopes).build();
		userController.CreateUsers(testUser);

		AssertionErrors.assertEquals("Check if Email is wrong","Hello@Gmail.com",userRepository.findByUsername("testUser").orElseThrow().getMail());
	}

}
