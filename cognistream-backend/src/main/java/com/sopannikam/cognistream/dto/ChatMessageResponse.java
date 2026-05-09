package com.sopannikam.cognistream.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sopannikam.cognistream.entity.MessageRole;

public record ChatMessageResponse(
        UUID id,
        MessageRole role,
        String content,
        List<CitationDto> citations,
        Instant createdAt) {
}
