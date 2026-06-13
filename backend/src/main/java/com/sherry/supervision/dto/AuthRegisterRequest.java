package com.sherry.supervision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
        @NotBlank
        @Size(min = 3, max = 80)
        String username,

        @JsonProperty("display_name")
        @NotBlank
        @Size(max = 120)
        String displayName,

        @NotBlank
        @Size(min = 8, max = 128)
        String password) {
}
