package com.karthikProjects.AuthServer.Userfolder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserService userService;
    private UserRepository mockRepository;

    @BeforeEach
    public void setUp() throws Exception {
        userService = new UserService();
        mockRepository = mock(UserRepository.class);
        // inject mock repository into the service via reflection
        Field repoField = UserService.class.getDeclaredField("repository");
        repoField.setAccessible(true);
        repoField.set(userService, mockRepository);
    }

    @Test
    public void loadUserByUsername_whenUserExists_returnsUserDetails() {
        ArrayList<Scopes> roles = new ArrayList<>();
        roles.add(Scopes.READ);
        Users user = Users.builder().username("alice").password("encoded-pass").mail("a@b.com").roles(roles).build();
        when(mockRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        var userDetails = userService.loadUserByUsername("alice");

        assertEquals("alice", userDetails.getUsername());
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_READ")));
    }

    @Test
    public void loadUserByUsername_whenNotFound_throws() {
        when(mockRepository.findByUsername("bob")).thenReturn(Optional.empty());
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("bob"));
    }

    @Test
    public void save_whenUserDoesNotExist_savesWithEncodedPassword() {
        ArrayList<Scopes> roles = new ArrayList<>();
        roles.add(Scopes.ADMIN);
        Users input = Users.builder().username("newuser").password("plainPass").mail("n@x.com").roles(roles).build();

        when(mockRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(mockRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.save(input);

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(mockRepository, times(1)).save(captor.capture());
        Users saved = captor.getValue();
        assertNotNull(saved.getPassword());
        assertNotEquals("plainPass", saved.getPassword());
        // check encoded password matches raw using encoder
        assertTrue(com.karthikProjects.AuthServer.Configuration.ConfigClass.passwordEncoder().matches("plainPass", saved.getPassword()));
    }

    @Test
    public void save_whenUserExists_doesNotCallSave() {
        ArrayList<Scopes> roles = new ArrayList<>();
        roles.add(Scopes.ADMIN);
        Users existing = Users.builder().username("exists").password("enc").mail("e@e.com").roles(roles).build();
        when(mockRepository.findByUsername("exists")).thenReturn(Optional.of(existing));

        Users input = Users.builder().username("exists").password("anything").mail("e@e.com").roles(roles).build();
        userService.save(input);

        verify(mockRepository, never()).save(any(Users.class));
    }
}

