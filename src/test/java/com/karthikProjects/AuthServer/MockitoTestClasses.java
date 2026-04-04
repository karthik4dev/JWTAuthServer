package com.karthikProjects.AuthServer;

import com.karthikProjects.AuthServer.Userfolder.UserRepository;
import com.karthikProjects.AuthServer.Userfolder.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(classes = AuthServerApplication.class)
public class MockitoTestClasses {

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("check if default UserNameNotFound error is thrown")
    void assertMockitoTest(){
        //Setup the Mockito Bean
        Mockito.when(userRepository.findByUsername("Karthik"))
                .thenThrow(new UsernameNotFoundException("Username \"Karthik\" not found"));
        //Assertion
        Assertions.assertThrows(UsernameNotFoundException.class,() -> userService.loadUserByUsername("Karthik"),"Username \"Karthik\" not found");
        //verify the method calls
        Mockito.verify(userRepository,Mockito.times(1)).findByUsername("Karthik");
    }

}
