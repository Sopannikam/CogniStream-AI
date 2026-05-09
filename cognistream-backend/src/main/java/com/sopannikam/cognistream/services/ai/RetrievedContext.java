package com.sopannikam.cognistream.services.ai;

import java.util.List;

import com.sopannikam.cognistream.dto.CitationDto;

public record RetrievedContext(
        List<CitationDto> citations,
        String contextText) {
}
