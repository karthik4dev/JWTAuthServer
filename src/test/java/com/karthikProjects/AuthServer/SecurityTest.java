package com.karthikProjects.AuthServer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTest {
    private static final String GET_ACCESS_TOKEN_END_POINT="/oauth2/token";

    @Autowired
    MockMvc mockMvc;

    @Test
    public void testGetAccessTokenFail() throws Exception {
        mockMvc.perform(post(GET_ACCESS_TOKEN_END_POINT)
                        .param("client_id", "client1")
                        .param("client_secret", "password2")
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isUnauthorized())
                .andDo(print())

                .andExpect(jsonPath("$.error", is("invalid_client")));
    }

    @Test
    public void testGetAccessTokenSuccess() throws Exception {
        mockMvc.perform(post(GET_ACCESS_TOKEN_END_POINT)
                        .param("client_id", "client1")
                        .param("client_secret", "secret")
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.token_type", is("Bearer")))
                .andExpect(jsonPath("$.access_token").isString())
                .andExpect(jsonPath("$.expires_in").isNumber())
        ;
    }

}
