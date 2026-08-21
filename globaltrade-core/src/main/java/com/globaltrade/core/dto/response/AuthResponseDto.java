package com.globaltrade.core.dto.response;

import com.globaltrade.core.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto implements Serializable {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String username;
    private String email;
    private String fullName;
    private UserRole role;
}