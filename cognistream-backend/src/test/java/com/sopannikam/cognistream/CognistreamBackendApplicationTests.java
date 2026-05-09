package com.sopannikam.cognistream;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sopannikam.cognistream.entity.User;
import com.sopannikam.cognistream.security.AppUserPrincipal;
import com.sopannikam.cognistream.services.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cognistream-tests;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.vectorstore.pgvector.initialize-schema=false",
        "spring.ai.vectorstore.pgvector.schema-validation=false"
})
@AutoConfigureMockMvc
@MockitoBean(types = {ChatModel.class, EmbeddingModel.class, VectorStore.class})
class CognistreamBackendApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired UserService userService;

    @Test
    void protectedApiRejectsAnonymousRequests() throws Exception {
        mvc.perform(get("/api/repos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/chat/sessions").param("repositoryId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void githubProfileIsUpdatedAndTokensAreEncrypted() {
        User first = userService.upsertFromGitHub(Map.of("id", 100L, "login", "student"), "first-token", "repo");
        UUID id = first.getId();
        assertThat(first.getDisplayName()).isEqualTo("student");
        assertThat(first.getAccessToken()).isNotEqualTo("first-token");
        assertThat(userService.decryptAccessToken(first)).isEqualTo("first-token");
        User second = userService.upsertFromGitHub(Map.of("id", 100L, "login", "student", "name", "Updated"),
                "new-token", "repo");
        assertThat(second.getId()).isEqualTo(id);
        assertThat(second.getDisplayName()).isEqualTo("Updated");
        assertThat(userService.decryptAccessToken(second)).isEqualTo("new-token");
    }

    @Test
    void currentUserResponseNeverReturnsAccessToken() throws Exception {
        User user = userService.upsertFromGitHub(Map.of("id", 101L, "login", "owner"), "secret-token", "repo");
        mvc.perform(get("/api/auth/me").with(oauth2Login().oauth2User(new AppUserPrincipal(user, Map.of("id", 101L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("githubUsername").value("owner"))
                .andExpect(jsonPath("accessToken").doesNotExist());
    }

    @Test
    void repositoryOwnershipPreventsCrossUserAccess() throws Exception {
        User user = userService.upsertFromGitHub(Map.of("id", 102L, "login", "owner2"), "token", "repo");
        mvc.perform(get("/api/repos/" + UUID.randomUUID())
                .with(oauth2Login().oauth2User(new AppUserPrincipal(user, Map.of("id", 102L)))))
                .andExpect(status().isNotFound());
    }
}
