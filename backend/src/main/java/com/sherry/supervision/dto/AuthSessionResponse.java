package com.sherry.supervision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthSessionResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        AuthUserResponse user) {
}
