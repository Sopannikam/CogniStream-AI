package com.sopannikam.cognistream.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;

import com.sopannikam.cognistream.entity.IndexStatus;
import com.sopannikam.cognistream.entity.Repository;
import com.sopannikam.cognistream.entity.User;
import com.sopannikam.cognistream.exceptions.BadRequestException;
import com.sopannikam.cognistream.exceptions.NotFoundException;
import com.sopannikam.cognistream.repository.RepositoryRepository;
import com.sopannikam.cognistream.services.github.GithubApiClient;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RepoServiceTests {
    private final RepositoryRepository repositories = mock(RepositoryRepository.class);
    private final UserService users = mock(UserService.class);
    private final GithubApiClient github = mock(GithubApiClient.class);
    private final RepoService service = new RepoService(repositories, users, github);
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        User user = new User();
        when(users.requiredById(userId)).thenReturn(user);
        when(users.decryptAccessToken(user)).thenReturn("test-token");
        when(repositories.findByUserIdAndGithubRepoId(any(), anyLong())).thenReturn(Optional.empty());
        when(repositories.save(any())).thenAnswer(call -> {
            Repository repo = call.getArgument(0);
            if (repo.getId() == null) repo.setId(UUID.randomUUID());
            return repo;
        });
        when(github.getRepository("test-token", "octocat", "Hello-World")).thenReturn(publicRepo());
    }

    @Test
    void importsAnotherOwnersPublicRepositoryAndUsesItsDefaultBranch() {
        var result = service.importPublicRepository(userId, " https://github.com/octocat/Hello-World.git/ ");
        assertThat(result.fullName()).isEqualTo("octocat/Hello-World");
        assertThat(result.defaultBranch()).isEqualTo("master");
        assertThat(result.indexStatus()).isEqualTo(IndexStatus.PENDING);
        verify(repositories).save(argThat(repo -> repo.getUserId().equals(userId) && !repo.isPrivate()));
    }

    @Test
    void importingAnExistingRepositoryKeepsItsIndexAndIdentity() {
        Repository existing = Repository.builder().id(UUID.randomUUID()).userId(userId).githubRepoId(1296269L)
                .indexStatus(IndexStatus.READY).chunkCount(12).indexedAt(Instant.now()).build();
        when(repositories.findByUserIdAndGithubRepoId(userId, 1296269L)).thenReturn(Optional.of(existing));
        var result = service.importPublicRepository(userId, "https://github.com/octocat/Hello-World");
        assertThat(result.id()).isEqualTo(existing.getId());
        assertThat(result.indexStatus()).isEqualTo(IndexStatus.READY);
        assertThat(result.chunkCount()).isEqualTo(12);
    }

    @Test
    void rejectsPrivateRepositoriesEvenWhenTheTokenCanReadThem() {
        when(github.getRepository(anyString(), anyString(), anyString()))
                .thenReturn(Map.of("private", true));
        assertThatThrownBy(() -> service.importPublicRepository(userId, "https://github.com/octocat/Hello-World"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Only public");
        verify(repositories, never()).save(any());
    }

    @Test
    void rejectsNonRepositoryUrlsBeforeContactingGitHub() {
        for (String url : List.of("http://github.com/octocat/Hello-World", "https://github.com.evil.test/a/b",
                "https://github.com@evil.test/a/b", "https://github.com/octocat/Hello-World/tree/main",
                "https://github.com/a/%2e%2e", "https://github.com/a/..", "https://localhost/a/b",
                "https://github.com/a/b?redirect=elsewhere", "https://github.com:443/a/b", "invalid")) {
            assertThatThrownBy(() -> service.importPublicRepository(userId, url))
                    .as(url).isInstanceOf(BadRequestException.class);
        }
        verifyNoInteractions(github);
    }

    @Test
    void reportsMissingRepositoriesWithoutSavingThem() {
        when(github.getRepository(anyString(), anyString(), anyString())).thenThrow(
                HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));
        assertThatThrownBy(() -> service.importPublicRepository(userId, "https://github.com/octocat/Hello-World"))
                .isInstanceOf(NotFoundException.class).hasMessageContaining("Public repository not found");
        verify(repositories, never()).save(any());
    }

    @Test
    void syncingStillReturnsPublicImportsAbsentFromTheGitHubAccountList() {
        Repository imported = Repository.builder().id(UUID.randomUUID()).userId(userId).githubRepoId(1296269L)
                .owner("octocat").name("Hello-World").fullName("octocat/Hello-World").build();
        when(github.listUserRepos("test-token")).thenReturn(List.of());
        when(repositories.findByUserIdOrderByFullNameAsc(userId)).thenReturn(List.of(imported));
        assertThat(service.syncAndListRepos(userId)).extracting(repo -> repo.fullName())
                .containsExactly("octocat/Hello-World");
    }

    private Map<String, Object> publicRepo() {
        return Map.of("id", 1296269L, "full_name", "octocat/Hello-World", "private", false,
                "default_branch", "master", "html_url", "https://github.com/octocat/Hello-World");
    }
}
