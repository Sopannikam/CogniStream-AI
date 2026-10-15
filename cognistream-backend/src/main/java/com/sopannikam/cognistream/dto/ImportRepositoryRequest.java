package com.sopannikam.cognistream.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportRepositoryRequest(@NotBlank @Size(max = 500) String url) {
}
