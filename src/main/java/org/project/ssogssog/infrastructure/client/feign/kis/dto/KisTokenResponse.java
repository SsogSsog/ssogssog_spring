package org.project.ssogssog.infrastructure.client.feign.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString(exclude = "accessToken")
public class KisTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("access_token_token_expired")
    private String tokenExpired;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Long expiresIn;
}
