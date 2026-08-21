package com.globaltrade.core.service;

import com.globaltrade.core.dto.request.LoginRequestDto;
import com.globaltrade.core.dto.request.RegisterRequestDto;
import com.globaltrade.core.dto.request.TokenRefreshRequestDto;
import com.globaltrade.core.dto.response.AuthResponseDto;
import jakarta.ejb.Local;

@Local
public interface AuthService {

    void register(RegisterRequestDto request);

    AuthResponseDto login(LoginRequestDto request);

    AuthResponseDto refreshToken(TokenRefreshRequestDto request);

    void logout(TokenRefreshRequestDto request);
}