package com.globaltrade.web.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/health")
public class HealthCheckResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth() {
        return Response.ok(Map.of(
                "status", "UP",
                "service", "GlobalTrade SCM Platform",
                "server", "GlassFish Jakarta EE 10"
        )).build();
    }
}