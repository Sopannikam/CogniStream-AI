package com.sopannikam.cognistream.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.sopannikam.cognistream.entity.Repository;

public interface RepositoryRepository extends JpaRepository<Repository, UUID> {
    List<Repository> findByUserIdOrderByFullNameAsc(UUID userId);

    Optional<Repository> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Repository r where r.id = :id and r.userId = :userId")
    Optional<Repository> findForIndexing(@Param("id") UUID id, @Param("userId") UUID userId);

    Optional<Repository> findByUserIdAndGithubRepoId(UUID userId, Long githubRepoId);
}
