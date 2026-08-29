package com.globaltrade.web.controller;

import com.globaltrade.core.dto.request.RouteOptimizationRequestDto;
import com.globaltrade.core.dto.response.RouteOptimizationResponseDto;
import com.globaltrade.core.service.RouteOptimizationService;
import com.globaltrade.core.util.StandardResponseDto;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/routes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
public class RouteOptimizationController {

    @EJB
    private RouteOptimizationService routeOptimizationService;

    @POST
    @Path("/optimize/{shipmentId}")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR"})
    public Response calculateAndCreateOptimizedRoute(@PathParam("shipmentId") Long shipmentId,
                                                     @Valid @NotNull RouteOptimizationRequestDto request,
                                                     @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        RouteOptimizationResponseDto response = routeOptimizationService.calculateAndCreateOptimizedRoute(shipmentId, request, username);

        return Response.status(Response.Status.CREATED)
                .entity(StandardResponseDto.builder()
                        .code(Response.Status.CREATED.getStatusCode())
                        .message("Route optimization plan calculated and generated successfully")
                        .data(response)
                        .build())
                .build();
    }

    @GET
    @Path("/plans/{planReference}")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public Response getPlanByReference(@PathParam("planReference") String planReference) {
        RouteOptimizationResponseDto response = routeOptimizationService.getPlanByReference(planReference);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Route optimization plan retrieved successfully")
                .data(response)
                .build()).build();
    }

    @GET
    @Path("/shipment/{trackingNumber}")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public Response getPlansByShipmentTracking(@PathParam("trackingNumber") String trackingNumber) {
        List<RouteOptimizationResponseDto> response = routeOptimizationService.getPlansByShipmentTracking(trackingNumber);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Route plans for shipment retrieved successfully")
                .data(response)
                .build()).build();
    }

    @GET
    @Path("/plans")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public Response getAllRoutePlans() {
        List<RouteOptimizationResponseDto> response = routeOptimizationService.getAllRoutePlans();

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("All route optimization plans retrieved successfully")
                .data(response)
                .build()).build();
    }

    @POST
    @Path("/plans/{planReference}/activate")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR"})
    public Response activateRoutePlan(@PathParam("planReference") String planReference,
                                      @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        RouteOptimizationResponseDto response = routeOptimizationService.activateRoutePlan(planReference, username);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Route optimization plan activated and dispatched to carrier")
                .data(response)
                .build()).build();
    }

    private String getAuthenticatedUsername(SecurityContext securityContext) {
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            return securityContext.getUserPrincipal().getName();
        }
        return "SYSTEM_USER";
    }
}