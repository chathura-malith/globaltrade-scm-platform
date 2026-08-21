package com.globaltrade.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefreshRequestDto implements Serializable {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}