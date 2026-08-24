package com.globaltrade.web.controller;

import com.globaltrade.core.dto.request.WarehouseRequestDto;
import com.globaltrade.core.dto.response.WarehouseResponseDto;
import com.globaltrade.core.service.WarehouseService;
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

@Path("/warehouses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
public class WarehouseController {

    @EJB
    private WarehouseService warehouseService;

    @POST
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN"})
    public Response createWarehouse(@Valid @NotNull WarehouseRequestDto request,
                                    @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        WarehouseResponseDto response = warehouseService.createWarehouse(request, username);

        return Response.status(Response.Status.CREATED)
                .entity(StandardResponseDto.builder()
                        .code(Response.Status.CREATED.getStatusCode())
                        .message("Warehouse registered successfully")
                        .data(response)
                        .build())
                .build();
    }

    @GET
    @Path("/{warehouseCode}")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public Response getWarehouseByCode(@PathParam("warehouseCode") String warehouseCode) {
        WarehouseResponseDto response = warehouseService.getWarehouseByCode(warehouseCode);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Warehouse retrieved successfully")
                .data(response)
                .build()).build();
    }

    @GET
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public Response getAllWarehouses() {
        List<WarehouseResponseDto> response = warehouseService.getAllWarehouses();

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("All warehouses retrieved successfully")
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