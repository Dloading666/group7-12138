package com.rpa.management;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndPermissionSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminLoginAndMeShouldWork() throws Exception {
        String token = login("admin", "admin123");

        String meResponse = mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(meResponse);
        assertThat(root.get("code").asInt()).isEqualTo(200);
        assertThat(root.path("data").path("permissionCodes").size()).isGreaterThan(0);
    }

    @Test
    void unauthorizedUsersEndpointShouldBeRejected() throws Exception {
        mockMvc.perform(get("/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanReadPermissionTree() throws Exception {
        String token = login("admin", "admin123");

        String response = mockMvc.perform(get("/permissions/tree")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asInt()).isEqualTo(200);
        assertThat(root.path("data").size()).isGreaterThan(0);
    }

    @Test
    void authenticatedUserCanUpdateOwnProfile() throws Exception {
        String token = login("admin", "admin123");

        String updateResponse = putJson("/user/profile", """
                {
                  "realName":"Admin Updated",
                  "phone":"13900001111",
                  "email":"admin-updated@example.com",
                  "avatar":"http://localhost:8080/api/user/avatar/test.png"
                }
                """, token)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode updateRoot = objectMapper.readTree(updateResponse);
        assertThat(updateRoot.get("code").asInt()).isEqualTo(200);
        assertThat(updateRoot.path("data").path("realName").asText()).isEqualTo("Admin Updated");
        assertThat(updateRoot.path("data").path("phone").asText()).isEqualTo("13900001111");
        assertThat(updateRoot.path("data").path("email").asText()).isEqualTo("admin-updated@example.com");

        String meResponse = mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode meRoot = objectMapper.readTree(meResponse);
        assertThat(meRoot.path("data").path("user").path("realName").asText()).isEqualTo("Admin Updated");
        assertThat(meRoot.path("data").path("user").path("phone").asText()).isEqualTo("13900001111");
        assertThat(meRoot.path("data").path("user").path("email").asText()).isEqualTo("admin-updated@example.com");
    }

    @Test
    void userCanChangeOwnPasswordWithOldPassword() throws Exception {
        String token = login("user01", "user123");

        String response = putJson("/user/password", """
                {
                  "oldPassword":"user123",
                  "newPassword":"user456"
                }
                """, token)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asInt()).isEqualTo(200);

        String newToken = login("user01", "user456");
        assertThat(newToken).isNotBlank();
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"%s"}
                    """.formatted(username, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("accessToken").asText();
    }

    private org.springframework.test.web.servlet.ResultActions putJson(String path, String json, String token) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
    }
}
