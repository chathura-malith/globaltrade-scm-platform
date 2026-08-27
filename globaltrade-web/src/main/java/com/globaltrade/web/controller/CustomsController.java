package com.globaltrade.web.controller;

import com.globaltrade.core.dto.request.CustomsClearanceActionDto;
import com.globaltrade.core.dto.response.CustomsDeclarationResponseDto;
import com.globaltrade.core.service.CustomsService;
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

@Path("/customs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
public class CustomsController {

    @EJB
    private CustomsService customsService;

    @POST
    @Path("/declarations/generate/{shipmentId}")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR"})
    public Response generateCustomsDocumentation(@PathParam("shipmentId") Long shipmentId,
                                                 @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        CustomsDeclarationResponseDto response = customsService.generateCustomsDocumentation(shipmentId, username);

        return Response.status(Response.Status.CREATED)
                .entity(StandardResponseDto.builder()
                        .code(Response.Status.CREATED.getStatusCode())
                        .message("Customs documentation bundle and declaration generated successfully")
                        .data(response)
                        .build())
                .build();
    }

    @GET
    @Path("/declarations/{declarationNumber}")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public Response getDeclarationByNumber(@PathParam("declarationNumber") String declarationNumber) {
        CustomsDeclarationResponseDto response = customsService.getDeclarationByNumber(declarationNumber);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Customs declaration retrieved successfully")
                .data(response)
                .build()).build();
    }

    @GET
    @Path("/declarations/shipment/{trackingNumber}")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public Response getDeclarationByTracking(@PathParam("trackingNumber") String trackingNumber) {
        CustomsDeclarationResponseDto response = customsService.getDeclarationByShipmentTracking(trackingNumber);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Customs declaration for shipment retrieved successfully")
                .data(response)
                .build()).build();
    }

    @GET
    @Path("/declarations")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public Response getAllDeclarations() {
        List<CustomsDeclarationResponseDto> response = customsService.getAllDeclarations();

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("All customs declarations retrieved successfully")
                .data(response)
                .build()).build();
    }

    @POST
    @Path("/declarations/{declarationNumber}/clearance")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public Response processCustomsClearance(@PathParam("declarationNumber") String declarationNumber,
                                            @Valid @NotNull CustomsClearanceActionDto action,
                                            @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        CustomsDeclarationResponseDto response = customsService.processCustomsClearance(declarationNumber, action, username);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Customs clearance decision recorded successfully")
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