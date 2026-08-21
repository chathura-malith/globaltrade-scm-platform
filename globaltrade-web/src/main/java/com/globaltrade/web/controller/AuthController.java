package com.globaltrade.web.controller;

import com.globaltrade.core.dto.request.LoginRequestDto;
import com.globaltrade.core.dto.request.RegisterRequestDto;
import com.globaltrade.core.dto.request.TokenRefreshRequestDto;
import com.globaltrade.core.dto.response.AuthResponseDto;
import com.globaltrade.core.exception.InvalidInputException;
import com.globaltrade.core.service.AuthService;
import com.globaltrade.core.util.StandardResponseDto;
import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    @EJB
    private AuthService authService;

    @POST
    @Path("/register")
    public Response register(@Valid RegisterRequestDto request) {
        if (request == null) {
            throw new InvalidInputException("Request body is required", 400);
        }
        authService.register(request);
        return Response.status(Response.Status.CREATED)
                .entity(new StandardResponseDto(201, "User registered successfully", null))
                .build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequestDto request) {
        if (request == null) {
            throw new InvalidInputException("Login credentials are required", 400);
        }
        AuthResponseDto responseDto = authService.login(request);
        return Response.status(Response.Status.OK)
                .entity(new StandardResponseDto(200, "Authentication successful", responseDto))
                .build();
    }

    @POST
    @Path("/refresh")
    public Response refresh(@Valid TokenRefreshRequestDto request) {
        if (request == null) {
            throw new InvalidInputException("Refresh token is required", 400);
        }
        AuthResponseDto responseDto = authService.refreshToken(request);
        return Response.status(Response.Status.OK)
                .entity(new StandardResponseDto(200, "Token refreshed successfully", responseDto))
                .build();
    }

    @POST
    @Path("/logout")
    public Response logout(@Valid TokenRefreshRequestDto request) {
        if (request == null) {
            throw new InvalidInputException("Refresh token is required for logout", 400);
        }
        authService.logout(request);
        return Response.status(Response.Status.OK)
                .entity(new StandardResponseDto(200, "Logged out successfully", null))
                .build();
    }
}