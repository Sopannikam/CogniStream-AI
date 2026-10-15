package com.sopannikam.cognistream.services.indexing;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import com.sopannikam.cognistream.entity.IndexStatus;
import com.sopannikam.cognistream.entity.Repository;
import com.sopannikam.cognistream.entity.User;
import com.sopannikam.cognistream.repository.RepositoryRepository;
import com.sopannikam.cognistream.services.UserService;
import com.sopannikam.cognistream.services.github.GitHubRateLimiter;
import com.sopannikam.cognistream.services.github.GithubApiClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IndexingServiceTests {
    RepositoryRepository repositories = mock(RepositoryRepository.class);
    UserService users = mock(UserService.class);
    GithubApiClient github = mock(GithubApiClient.class);
    CodeChunker chunker = mock(CodeChunker.class);
    VectorStore vectors = mock(VectorStore.class);
    UUID repoId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Repository repo;
    IndexingService service;

    @BeforeEach
    void setup() {
        repo = Repository.builder().id(repoId).userId(userId).owner("owner").name("project")
                .fullName("owner/project").defaultBranch("feature/code").indexStatus(IndexStatus.INDEXING).build();
        service = new IndexingService(repositories, users, github, new CodeFileFilter(), chunker,
                new GitHubRateLimiter(0), vectors);
        ReflectionTestUtils.setField(service, "maxFileBytes", 102400L);
        when(repositories.findByIdAndUserId(repoId, userId)).thenReturn(Optional.of(repo));
        when(repositories.findById(repoId)).thenReturn(Optional.of(repo));
        User user = new User();
        when(users.requiredById(userId)).thenReturn(user);
        when(users.decryptAccessToken(user)).thenReturn("test-token");
        when(github.getRepoTree(anyString(), anyString(), anyString(), anyString())).thenReturn(Map.of(
                "tree", List.of(Map.of("type", "blob", "path", "src/App.java", "size", 100))));
        when(github.getFileContent(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("class App {}");
        when(chunker.chunkFile(anyString(), anyString(), anyString())).thenReturn(List.of(new Document("class App {}")));
    }

    @Test
    void indexesTheSelectedBranchAndMarksReadyAfterVectorsAreStored() {
        service.indexAsync(repoId, userId);
        verify(github).getFileContent("test-token", "owner", "project", "src/App.java", "feature/code");
        verify(vectors).add(anyList());
        assertThat(repo.getIndexStatus()).isEqualTo(IndexStatus.READY);
        assertThat(repo.getChunkCount()).isEqualTo(1);
    }

    @Test
    void aLargeFileIsEmbeddedInBoundedBatchesWithoutDroppingChunks() {
        List<Document> chunks = IntStream.range(0, 11)
                .mapToObj(i -> new Document("chunk " + i)).toList();
        when(chunker.chunkFile(anyString(), anyString(), anyString())).thenReturn(chunks);
        List<Document> stored = new ArrayList<>();
        doAnswer(invocation -> {
            List<Document> batch = invocation.getArgument(0);
            assertThat(batch).hasSizeLessThanOrEqualTo(4);
            stored.addAll(batch);
            return null;
        }).when(vectors).add(anyList());

        service.indexAsync(repoId, userId);

        assertThat(stored).containsExactlyElementsOf(chunks);
        assertThat(repo.getIndexStatus()).isEqualTo(IndexStatus.READY);
        assertThat(repo.getChunkCount()).isEqualTo(chunks.size());
    }

    @Test
    void embeddingFailureNeverMarksTheRepositoryReady() {
        doThrow(new IllegalStateException("Embedding API unavailable")).when(vectors).add(anyList());
        service.indexAsync(repoId, userId);
        assertThat(repo.getIndexStatus()).isEqualTo(IndexStatus.FAILED);
        assertThat(repo.getErrorMessage()).contains("Embedding API unavailable");
    }

    @Test
    void emptyRepositoryFailsInsteadOfAllowingUnsupportedChat() {
        when(github.getRepoTree(anyString(), anyString(), anyString(), anyString())).thenReturn(Map.of("tree", List.of()));
        service.indexAsync(repoId, userId);
        assertThat(repo.getIndexStatus()).isEqualTo(IndexStatus.FAILED);
        verify(vectors, never()).add(anyList());
    }
}
