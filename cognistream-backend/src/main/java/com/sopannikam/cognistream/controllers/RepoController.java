package com.sopannikam.cognistream.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.core.task.TaskRejectedException;
import com.sopannikam.cognistream.exceptions.ServiceUnavailableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import com.sopannikam.cognistream.dto.ImportRepositoryRequest;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sopannikam.cognistream.dto.IndexStatusResponse;
import com.sopannikam.cognistream.dto.RepositoryResponse;
import com.sopannikam.cognistream.entity.Repository;
import com.sopannikam.cognistream.security.CurrentUser;
import com.sopannikam.cognistream.services.RepoService;
import com.sopannikam.cognistream.services.indexing.IndexingService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepoController {

    private final CurrentUser currentUser;
    private final RepoService repoService;

    private final IndexingService indexingService;

    @GetMapping
    public List<RepositoryResponse> list(
            @RequestParam(name = "refresh", defaultValue = "true") boolean refresh) {
        UUID userId = currentUser.require().getId();
        if (refresh) {
            return repoService.syncAndListRepos(userId);
        }
        return repoService.listStored(userId);
    }

    @PostMapping("/import")
    public RepositoryResponse importPublic(@Valid @RequestBody ImportRepositoryRequest request) {
        UUID userId = currentUser.require().getId();
        return repoService.importPublicRepository(userId, request.url());
    }

    @GetMapping("/{id}")
    public RepositoryResponse get(@PathVariable UUID id) {
        UUID userId = currentUser.require().getId();
        return repoService.toResponse(repoService.requireOwned(id, userId));
    }

    @PostMapping("/{id}/index")
    public ResponseEntity<RepositoryResponse> index(@PathVariable UUID id) {
        UUID userId = currentUser.require().getId();
        Repository repo = indexingService.startIndexing(id, userId);
        try {
            indexingService.indexAsync(id, userId);
        } catch (TaskRejectedException ex) {
            String message = "Indexing queue is full. Please try again later.";
            indexingService.markFailed(id, message);
            throw new ServiceUnavailableException(message);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(repoService.toResponse(repo));
    }

    @GetMapping("/{id}/status")
    public IndexStatusResponse status(@PathVariable UUID id) {
        UUID userId = currentUser.require().getId();
        return repoService.status(id, userId);
    }

}
