package com.sopannikam.cognistream.controllers;

import java.util.UUID;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import com.sopannikam.cognistream.entity.Repository;
import com.sopannikam.cognistream.entity.User;
import com.sopannikam.cognistream.exceptions.ServiceUnavailableException;
import com.sopannikam.cognistream.security.CurrentUser;
import com.sopannikam.cognistream.security.AppUserPrincipal;
import com.sopannikam.cognistream.services.RepoService;
import com.sopannikam.cognistream.services.indexing.IndexingService;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RepoControllerTests {
    @Test
    void aFullQueueMarksTheJobFailedSoItCanBeRetried() {
        var currentUser = mock(CurrentUser.class);
        var repos = mock(RepoService.class);
        var indexing = mock(IndexingService.class);
        var controller = new RepoController(currentUser, repos, indexing);
        UUID userId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        User user = new User(); user.setId(userId);
        when(currentUser.require()).thenReturn(new AppUserPrincipal(user, Map.of("id", 123L)));
        when(indexing.startIndexing(repoId, userId)).thenReturn(new Repository());
        doThrow(new TaskRejectedException("Full")).when(indexing).indexAsync(repoId, userId);
        assertThatThrownBy(() -> controller.index(repoId)).isInstanceOf(ServiceUnavailableException.class);
        verify(indexing).markFailed(repoId, "Indexing queue is full. Please try again later.");
        verifyNoInteractions(repos);
    }
}
