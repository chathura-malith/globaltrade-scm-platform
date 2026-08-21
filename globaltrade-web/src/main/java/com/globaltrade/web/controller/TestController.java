package com.globaltrade.web.controller;

import com.globaltrade.core.util.StandardResponseDto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
public class TestController {

    @GET
    @Path("/public")
    public Response publicEndpoint() {
        return Response.ok(new StandardResponseDto(200, "Public endpoint accessed successfully", null)).build();
    }

    @GET
    @Path("/secured")
    public Response securedEndpoint(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        return Response.ok(new StandardResponseDto(200, "Protected endpoint accessed by: " + username, null)).build();
    }

    @GET
    @Path("/admin-only")
    @RolesAllowed("SYSTEM_ADMIN")
    public Response adminEndpoint(@Context SecurityContext securityContext) {
        return Response.ok(new StandardResponseDto(200, "SYSTEM_ADMIN authorized access granted", null)).build();
    }
}