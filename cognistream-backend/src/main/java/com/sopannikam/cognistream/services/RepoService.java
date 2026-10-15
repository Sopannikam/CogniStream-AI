package com.sopannikam.cognistream.services;

import java.time.Instant;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import com.sopannikam.cognistream.exceptions.BadRequestException;
import org.springframework.transaction.annotation.Transactional;

import com.sopannikam.cognistream.dto.IndexStatusResponse;
import com.sopannikam.cognistream.dto.RepositoryResponse;
import com.sopannikam.cognistream.entity.Repository;
import com.sopannikam.cognistream.entity.User;
import com.sopannikam.cognistream.exceptions.NotFoundException;
import com.sopannikam.cognistream.repository.RepositoryRepository;
import com.sopannikam.cognistream.services.github.GithubApiClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RepoService {
    private final RepositoryRepository repositoryRepository;
    private final UserService userService;
    private final GithubApiClient gitHubApiClient;

    @Transactional
    public List<RepositoryResponse> syncAndListRepos(UUID userId) {
        User user = userService.requiredById(userId);
        String token = userService.decryptAccessToken(user);
        List<Map<String, Object>> remoteRepos = gitHubApiClient.listUserRepos(token);

        for (Map<String, Object> remote : remoteRepos) {
            saveRepository(userId, remote);
        }
        // Public imports may not appear in /user/repos; keep them in the workspace.
        return listStored(userId);
    }

    @Transactional
    public RepositoryResponse importPublicRepository(UUID userId, String url) {
        String[] parts = parseRepositoryUrl(url);
        User user = userService.requiredById(userId);
        String token = userService.decryptAccessToken(user);
        Map<String, Object> remote;
        try {
            remote = gitHubApiClient.getRepository(token, parts[0], parts[1]);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new NotFoundException("Public repository not found. Check the GitHub URL.");
            }
            if (ex.getStatusCode().value() == 403 || ex.getStatusCode().value() == 429) {
                throw new BadRequestException("GitHub could not read this repository. Check access or try again later.");
            }
            throw ex;
        }
        if (remote == null || !Boolean.FALSE.equals(remote.get("private"))) {
            throw new BadRequestException("Only public GitHub repositories can be imported by URL.");
        }
        return toResponse(saveRepository(userId, remote));
    }

    private static String[] parseRepositoryUrl(String url) {
        try {
            URI uri = URI.create(url == null ? "" : url.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"github.com".equalsIgnoreCase(uri.getHost())
                    || uri.getUserInfo() != null || uri.getPort() != -1
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException();
            }
            String path = uri.getRawPath().replaceFirst("/$", "");
            String[] parts = path.split("/", -1);
            if (parts.length != 3) throw new IllegalArgumentException();
            String owner = parts[1];
            String name = parts[2].replaceFirst("\\.git$", "");
            if (!owner.matches("[A-Za-z0-9][A-Za-z0-9-]{0,38}")
                    || !name.matches("[A-Za-z0-9_.-]{1,100}")
                    || name.equals(".") || name.equals("..")) {
                throw new IllegalArgumentException();
            }
            return new String[] {owner, name};
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Enter a repository URL like https://github.com/owner/repository.");
        }
    }

    private Repository saveRepository(UUID userId, Map<String, Object> remote) {
        Long githubRepoId = toLong(remote.get("id"));
        Repository repo = repositoryRepository.findByUserIdAndGithubRepoId(userId, githubRepoId)
                .orElseGet(Repository::new);
        String fullName = String.valueOf(remote.get("full_name"));
        String[] parts = fullName.split("/", 2);
        repo.setUserId(userId);
        repo.setGithubRepoId(githubRepoId);
        repo.setOwner(parts[0]);
        repo.setName(parts[1]);
        repo.setFullName(fullName);
        repo.setPrivate(Boolean.TRUE.equals(remote.get("private")));
        repo.setDefaultBranch(remote.get("default_branch") != null
                ? String.valueOf(remote.get("default_branch")) : "main");
        repo.setLanguage(remote.get("language") != null ? String.valueOf(remote.get("language")) : null);
        repo.setHtmlUrl(remote.get("html_url") != null ? String.valueOf(remote.get("html_url")) : null);
        repo.setDescription(remote.get("description") != null ? String.valueOf(remote.get("description")) : null);
        repo.setUpdatedAt(Instant.now());
        return repositoryRepository.save(repo);
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> listStored(UUID userId) {
        return repositoryRepository.findByUserIdOrderByFullNameAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Repository requireOwned(UUID repoId, UUID userId) {
        return repositoryRepository.findByIdAndUserId(repoId, userId)
                .orElseThrow(() -> new NotFoundException("Repository not found"));
    }


    @Transactional(readOnly = true)
    public IndexStatusResponse status(UUID repoId, UUID userId) {
        Repository repo = requireOwned(repoId, userId);
        return new IndexStatusResponse(
                repo.getId(),
                repo.getIndexStatus(),
                repo.getFilesTotal(),
                repo.getFilesProcessed(),
                repo.getChunkCount(),
                repo.getIndexedAt(),
                repo.getErrorMessage());
    }

     public RepositoryResponse toResponse(Repository repo) {
        return new RepositoryResponse(
                repo.getId(),
                repo.getGithubRepoId(),
                repo.getOwner(),
                repo.getName(),
                repo.getFullName(),
                repo.isPrivate(),
                repo.getDefaultBranch(),
                repo.getLanguage(),
                repo.getHtmlUrl(),
                repo.getDescription(),
                repo.getIndexStatus(),
                repo.getIndexedAt(),
                repo.getChunkCount(),
                repo.getFilesTotal(),
                repo.getFilesProcessed(),
                repo.getErrorMessage());
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
