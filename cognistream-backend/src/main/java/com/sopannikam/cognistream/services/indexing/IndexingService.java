package com.sopannikam.cognistream.services.indexing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

import com.sopannikam.cognistream.entity.IndexStatus;
import com.sopannikam.cognistream.entity.Repository;
import com.sopannikam.cognistream.exceptions.BadRequestException;
import com.sopannikam.cognistream.exceptions.NotFoundException;
import com.sopannikam.cognistream.repository.RepositoryRepository;
import com.sopannikam.cognistream.services.UserService;
import com.sopannikam.cognistream.services.ai.RagSettings;
import com.sopannikam.cognistream.services.github.GitHubRateLimiter;
import com.sopannikam.cognistream.services.github.GithubApiClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {
    private static final int VECTOR_BATCH_SIZE = 4;
    private static final int PROGRESS_EVERY_N_FILES = 5;

    private final RepositoryRepository repositoryRepository;
    private final UserService userService;
    private final GithubApiClient gitHubApiClient;
    private final CodeFileFilter fileFilter;
    private final CodeChunker codeChunker;
    private final GitHubRateLimiter rateLimiter;
    private final VectorStore vectorStore;

    @Value("${app.indexing.max-file-bytes:102400}")
    private long maxFileBytes;

    @Transactional
    public Repository startIndexing(UUID repoId, UUID userId) {
        Repository repo = repositoryRepository.findForIndexing(repoId, userId)
                .orElseThrow(() -> new NotFoundException("Repository not found"));

        if (repo.getIndexStatus() == IndexStatus.INDEXING) {
            throw new BadRequestException("Repository is already being indexed");
        }

        repo.setIndexStatus(IndexStatus.INDEXING);
        repo.setFilesProcessed(0);
        repo.setFilesTotal(0);
        repo.setChunkCount(0);
        repo.setErrorMessage(null);
        repo.setUpdatedAt(Instant.now());
        return repositoryRepository.save(repo);
    }

    @Async("indexingExecutor")
     public void indexAsync(UUID repoId, UUID userId) {
        try {
            doIndex(repoId, userId);
        } catch (Exception ex) {
            log.error("Indexing failed for repo {}", repoId, ex);
            markFailed(repoId, ex.getMessage());
        }
    }


      private void doIndex(UUID repoId, UUID userId) {
        Repository repo = repositoryRepository.findById(repoId)
                .orElseThrow(() -> new NotFoundException("Repository not found"));
        String token = userService.decryptAccessToken(userService.requiredById(userId));

        deleteExistingVectors(repoId.toString());

        Map<String, Object> tree = gitHubApiClient.getRepoTree(
                token, repo.getOwner(), repo.getName(), repo.getDefaultBranch());
        if (tree == null || Boolean.TRUE.equals(tree.get("truncated"))) {
            throw new BadRequestException("GitHub could not return the complete repository tree");
        }
        List<String> filePaths = listIndexableFiles(tree);
        if (filePaths.isEmpty()) {
            throw new BadRequestException("No supported code files found in this repository");
        }

        updateProgress(repoId, filePaths.size(), 0, 0, IndexStatus.INDEXING, null);

        List<Document> batch = new ArrayList<>();
        int processed = 0;
        int totalChunks = 0;

        for (String path : filePaths) {
            try {
                String content = gitHubApiClient.getFileContent(
                        token, repo.getOwner(), repo.getName(), path, repo.getDefaultBranch());
                List<Document> chunks = codeChunker.chunkFile(repoId.toString(), path, content);
                batch.addAll(chunks);
                totalChunks += chunks.size();
            } catch (Exception ex) {
                log.warn("Skipping file {} in {}: {}", path, repo.getFullName(), ex.getMessage());
            }

            // Let an embedding failure fail the job instead of claiming READY.
            while (batch.size() >= VECTOR_BATCH_SIZE) {
                vectorStore.add(List.copyOf(batch.subList(0, VECTOR_BATCH_SIZE)));
                batch.subList(0, VECTOR_BATCH_SIZE).clear();
            }

            processed++;
            if (processed % PROGRESS_EVERY_N_FILES == 0 || processed == filePaths.size()) {
                updateProgress(repoId, filePaths.size(), processed, totalChunks, IndexStatus.INDEXING, null);
            }
            rateLimiter.pause();
        }

        if (!batch.isEmpty()) {
            vectorStore.add(batch);
        }

        if (totalChunks == 0) {
            throw new BadRequestException("No code could be indexed; check repository access and try again");
        }

        markReady(repoId, filePaths.size(), processed, totalChunks, repo.getFullName());
    }


       @SuppressWarnings("unchecked")
    private List<String> listIndexableFiles(Map<String, Object> tree) {
        if (tree == null || tree.get("tree") == null) {
            return List.of();
        }

        List<Map<String, Object>> entries = (List<Map<String, Object>>) tree.get("tree");
        return entries.stream()
                .filter(entry -> "blob".equals(String.valueOf(entry.get("type"))))
                .filter(entry -> {
                    String path = String.valueOf(entry.get("path"));
                    long size = entry.get("size") instanceof Number n ? n.longValue() : 0L;
                    return fileFilter.isEligible(path, size, maxFileBytes);
                })
                .map(entry -> String.valueOf(entry.get("path")))
                .toList();
    }

     private void deleteExistingVectors(String repoId) {
        var filter = new FilterExpressionBuilder().eq(RagSettings.METADATA_REPO_ID, repoId).build();
        vectorStore.delete(filter);
    };

      @Transactional
    protected void updateProgress(
            UUID repoId,
            int total,
            int processed,
            int chunks,
            IndexStatus status,
            String error) {
        repositoryRepository.findById(repoId).ifPresent(repo -> {
            repo.setFilesTotal(total);
            repo.setFilesProcessed(processed);
            repo.setChunkCount(chunks);
            repo.setIndexStatus(status);
            repo.setErrorMessage(error);
            repo.setUpdatedAt(Instant.now());
            repositoryRepository.save(repo);
        });
    }

      @Transactional
    protected void markReady(UUID repoId, int totalFiles, int processedFiles, int totalChunks, String fullName) {
        repositoryRepository.findById(repoId).ifPresent(repo -> {
            repo.setIndexStatus(IndexStatus.READY);
            repo.setFilesTotal(totalFiles);
            repo.setFilesProcessed(processedFiles);
            repo.setChunkCount(totalChunks);
            repo.setIndexedAt(Instant.now());
            repo.setErrorMessage(null);
            repo.setUpdatedAt(Instant.now());
            repositoryRepository.save(repo);
        });
        log.info("Indexed {} files ({} chunks) for {}", processedFiles, totalChunks, fullName);
    }

     @Transactional
    public void markFailed(UUID repoId, String message) {
        repositoryRepository.findById(repoId).ifPresent(repo -> {
            repo.setIndexStatus(IndexStatus.FAILED);
            repo.setErrorMessage(message != null && message.length() > 2000
                    ? message.substring(0, 2000)
                    : message);
            repo.setUpdatedAt(Instant.now());
            repositoryRepository.save(repo);
        });
    }

}
