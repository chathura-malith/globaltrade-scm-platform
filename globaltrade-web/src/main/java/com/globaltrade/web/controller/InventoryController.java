package com.globaltrade.web.controller;

import com.globaltrade.core.dto.request.ItemStockRequestDto;
import com.globaltrade.core.dto.request.StockAdjustmentRequestDto;
import com.globaltrade.core.dto.request.StockOperationRequestDto;
import com.globaltrade.core.dto.response.ItemStockResponseDto;
import com.globaltrade.core.dto.response.StockReplenishmentResponseDto;
import com.globaltrade.core.service.InventoryService;
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

@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
public class InventoryController {

    @EJB
    private InventoryService inventoryService;

    @POST
    @Path("/items")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "WAREHOUSE_MANAGER"})
    public Response createOrUpdateStock(@Valid @NotNull ItemStockRequestDto request,
                                        @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        ItemStockResponseDto response = inventoryService.createOrUpdateItemStock(request, username);

        return Response.status(Response.Status.CREATED)
                .entity(StandardResponseDto.builder()
                        .code(Response.Status.CREATED.getStatusCode())
                        .message("Item stock created/updated successfully")
                        .data(response)
                        .build())
                .build();
    }

    @GET
    @Path("/items/{sku}")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public Response getItemStockBySku(@PathParam("sku") String sku) {
        ItemStockResponseDto response = inventoryService.getItemStockBySku(sku);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Item stock retrieved successfully")
                .data(response)
                .build()).build();
    }

    @GET
    @Path("/warehouse/{warehouseCode}")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public Response getStocksByWarehouse(@PathParam("warehouseCode") String warehouseCode) {
        List<ItemStockResponseDto> response = inventoryService.getStocksByWarehouse(warehouseCode);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Warehouse stocks retrieved successfully")
                .data(response)
                .build()).build();
    }

    @POST
    @Path("/adjust")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "WAREHOUSE_MANAGER"})
    public Response adjustStock(@Valid @NotNull StockAdjustmentRequestDto request,
                                @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        ItemStockResponseDto response = inventoryService.adjustStock(request, username);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Stock adjusted successfully")
                .data(response)
                .build()).build();
    }

    @POST
    @Path("/reserve")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public Response reserveStock(@Valid @NotNull StockOperationRequestDto request,
                                 @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        ItemStockResponseDto response = inventoryService.reserveStock(request.getSku(), request.getQuantity(), username);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Stock reserved successfully")
                .data(response)
                .build()).build();
    }

    @POST
    @Path("/release")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public Response releaseStock(@Valid @NotNull StockOperationRequestDto request,
                                 @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        ItemStockResponseDto response = inventoryService.releaseStock(request.getSku(), request.getQuantity(), username);

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Stock released successfully")
                .data(response)
                .build()).build();
    }


    @GET
    @Path("/replenishments")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public Response getAllReplenishments() {
        List<StockReplenishmentResponseDto> response = inventoryService.getAllReplenishments();

        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Replenishment orders retrieved successfully")
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