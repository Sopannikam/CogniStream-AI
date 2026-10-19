package com.sopannikam.cognistream;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import com.sopannikam.cognistream.entity.IndexStatus;
import com.sopannikam.cognistream.entity.Repository;
import com.sopannikam.cognistream.entity.ChatSession;
import com.sopannikam.cognistream.repository.RepositoryRepository;
import com.sopannikam.cognistream.repository.ChatSessionRepository;
import com.sopannikam.cognistream.services.indexing.IndexingService;
import com.sopannikam.cognistream.exceptions.BadRequestException;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @Autowired RepositoryRepository repositories;
    @Autowired ChatSessionRepository sessions;
    @Autowired IndexingService indexingService;

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

    @Test
    void malformedIdsAndInvalidBodiesReturnBadRequest() throws Exception {
        User user = userService.upsertFromGitHub(Map.of("id", 110L, "login", "validation"), "token", "repo");
        var principal = new AppUserPrincipal(user, Map.of("id", 110L));
        mvc.perform(get("/api/repos/not-a-uuid").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("status").value(400));
        mvc.perform(post("/api/repos/import").with(oauth2Login().oauth2User(principal))
                .contentType(MediaType.APPLICATION_JSON).content("{invalid"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/repos/import").with(oauth2Login().oauth2User(principal))
                .contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/chat/sessions").with(oauth2Login().oauth2User(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repositoryId\":\"" + UUID.randomUUID() + "\",\"title\":\"" + "x".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/chat/sessions/" + UUID.randomUUID() + "/messages")
                .with(oauth2Login().oauth2User(principal)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + "x".repeat(8001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherUsersSessionCannotBeRead() throws Exception {
        User owner = userService.upsertFromGitHub(Map.of("id", 111L, "login", "session-owner"), "token", "repo");
        User other = userService.upsertFromGitHub(Map.of("id", 112L, "login", "other-user"), "token", "repo");
        ChatSession session = sessions.save(ChatSession.builder().userId(owner.getId())
                .repositoryId(UUID.randomUUID()).title("Private chat").build());
        mvc.perform(get("/api/chat/sessions/" + session.getId())
                .with(oauth2Login().oauth2User(new AppUserPrincipal(other, Map.of("id", 112L)))))
                .andExpect(status().isNotFound());
    }

    @Test
    void concurrentIndexRequestsAdmitOnlyOneJob() throws Exception {
        User user = userService.upsertFromGitHub(Map.of("id", 113L, "login", "concurrency"), "token", "repo");
        Repository repo = repositories.save(Repository.builder().userId(user.getId()).githubRepoId(99113L)
                .owner("owner").name("project").fullName("owner/project").defaultBranch("main").build());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> request = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start latch timed out");
            try { indexingService.startIndexing(repo.getId(), user.getId()); return true; }
            catch (BadRequestException expected) { return false; }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(request);
            var second = executor.submit(request);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(repositories.findById(repo.getId()).orElseThrow().getIndexStatus()).isEqualTo(IndexStatus.INDEXING);
    }
}
