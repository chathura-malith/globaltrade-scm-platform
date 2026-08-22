package com.globaltrade.web.controller;

import com.globaltrade.core.dto.request.ShipmentCheckpointRequestDto;
import com.globaltrade.core.dto.request.ShipmentRequestDto;
import com.globaltrade.core.dto.response.ShipmentCheckpointResponseDto;
import com.globaltrade.core.dto.response.ShipmentTrackNoResponseDto;
import com.globaltrade.core.dto.response.ShipmentTrackingResponseDto;
import com.globaltrade.core.enums.ShipmentStatus;
import com.globaltrade.core.service.ShipmentService;
import com.globaltrade.core.util.StandardResponseDto;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
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

@Path("/shipments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "WAREHOUSE_MANAGER", "VENDOR"})
public class ShipmentController {

    @EJB
    private ShipmentService shipmentService;

    @POST
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR"})
    public Response createShipment(@Valid @NotNull ShipmentRequestDto request,
                                   @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        ShipmentTrackNoResponseDto response = shipmentService.createShipment(request, username);

        return Response.status(Response.Status.CREATED)
                .entity(StandardResponseDto.builder()
                        .code(Response.Status.CREATED.getStatusCode())
                        .message("Shipment created successfully")
                        .data(response)
                        .build())
                .build();
    }

    @GET
    @Path("/{trackingNumber}")
    @PermitAll
    public Response getShipmentByTrackingNumber(@PathParam("trackingNumber") String trackingNumber) {
        ShipmentTrackingResponseDto response = shipmentService.getShipmentByTrackingNumber(trackingNumber);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Shipment details retrieved successfully")
                .data(response)
                .build()).build();
    }

    @GET
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "WAREHOUSE_MANAGER"})
    public Response getAllShipments(@QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("size") @DefaultValue("20") int size) {
        List<ShipmentTrackingResponseDto> shipments = shipmentService.getAllShipments(page, size);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("All shipments retrieved successfully")
                .data(shipments)
                .build()).build();
    }

    @POST
    @Path("/{trackingNumber}/checkpoints")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "WAREHOUSE_MANAGER"})
    public Response addCheckpoint(@PathParam("trackingNumber") String trackingNumber,
                                  @Valid @NotNull ShipmentCheckpointRequestDto request,
                                  @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        ShipmentCheckpointResponseDto response = shipmentService.addCheckpoint(trackingNumber, request, username);

        return Response.status(Response.Status.CREATED)
                .entity(StandardResponseDto.builder()
                        .code(Response.Status.CREATED.getStatusCode())
                        .message("Checkpoint added successfully")
                        .data(response)
                        .build())
                .build();
    }

    @PATCH
    @Path("/{trackingNumber}/status")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN"})
    public Response updateShipmentStatus(@PathParam("trackingNumber") String trackingNumber,
                                         @QueryParam("newStatus") @NotNull ShipmentStatus newStatus,
                                         @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        ShipmentTrackingResponseDto response = shipmentService.updateShipmentStatus(trackingNumber, newStatus, username);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Shipment status updated successfully")
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