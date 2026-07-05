package com.karthikProjects.AuthServer.Userfolder;

import com.karthikProjects.AuthServer.AuthServerApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AuthServerApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class UserServiceIntegrationTest {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
    }

    @Test
    public void save_and_load_user_endToEnd() {
        ArrayList<Scopes> roles = new ArrayList<>();
        roles.add(Scopes.ADMIN);
        Users input = Users.builder().username("integrationUser").password("pwd123").mail("i@i.com").roles(roles).build();

        boolean saved = userService.save(input);
        assertTrue(saved);

        var found = userRepository.findByUsername("integrationUser");
        assertTrue(found.isPresent());
        assertEquals("i@i.com", found.get().getMail());

        var userDetails = userService.loadUserByUsername("integrationUser");
        assertEquals("integrationUser", userDetails.getUsername());
    }
}

