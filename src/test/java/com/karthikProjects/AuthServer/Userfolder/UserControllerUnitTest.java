package com.karthikProjects.AuthServer.Userfolder;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class UserControllerUnitTest {

    @Test
    public void createUsers_delegatesToUserService() {
        UserService mockService = mock(UserService.class);
        UserController controller = new UserController();
        // inject mock via reflection
        try {
            java.lang.reflect.Field f = UserController.class.getDeclaredField("userService");
            f.setAccessible(true);
            f.set(controller, mockService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Users input = Users.builder().username("ctrlUser").password("p").mail("c@c.com").roles(new java.util.ArrayList<>()).build();

        controller.CreateUsers(input);

        verify(mockService, times(1)).save(input);
    }
}

