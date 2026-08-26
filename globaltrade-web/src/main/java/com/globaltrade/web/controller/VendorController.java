package com.globaltrade.web.controller;

import com.globaltrade.core.dto.request.AssignReplenishmentVendorRequestDto;
import com.globaltrade.core.dto.response.StockReplenishmentResponseDto;
import com.globaltrade.core.dto.response.VendorFulfillmentResponseDto;
import com.globaltrade.core.dto.response.VendorResponseDto;
import com.globaltrade.core.enums.VendorStatus;
import com.globaltrade.core.service.VendorService;
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

@Path("/vendors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "VENDOR"})
public class VendorController {

    @EJB
    private VendorService vendorService;

    @GET
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public Response getAllVendors() {
        List<VendorResponseDto> vendors = vendorService.getAllVendors();
        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("All registered vendor performance profiles retrieved successfully")
                .data(vendors)
                .build()).build();
    }

    @GET
    @Path("/{vendorCode}")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public Response getVendorByCode(@PathParam("vendorCode") String vendorCode) {
        VendorResponseDto response = vendorService.getVendorProfileByCode(vendorCode);
        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Vendor profile retrieved successfully")
                .data(response)
                .build()).build();
    }

    @GET
    @Path("/profile/me")
    @RolesAllowed({"VENDOR"})
    public Response getMyVendorProfile(@Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        VendorResponseDto response = vendorService.getVendorProfileByUsername(username);
        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Authenticated vendor profile retrieved successfully")
                .data(response)
                .build()).build();
    }

    @PUT
    @Path("/{vendorCode}/status")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN"})
    public Response updateVendorStatus(@PathParam("vendorCode") String vendorCode,
                                       @QueryParam("status") VendorStatus status,
                                       @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        VendorResponseDto response = vendorService.updateVendorStatus(vendorCode, status, username);
        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Vendor operational status updated to " + status)
                .data(response)
                .build()).build();
    }

    @POST
    @Path("/replenishments/assign")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public Response assignReplenishmentOrder(@Valid @NotNull AssignReplenishmentVendorRequestDto request,
                                             @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        StockReplenishmentResponseDto response = vendorService.assignReplenishmentOrder(request, username);
        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Replenishment order assigned to vendor successfully")
                .data(response)
                .build()).build();
    }

    @GET
    @Path("/replenishments/my-orders")
    @RolesAllowed({"VENDOR"})
    public Response getMyAssignedReplenishments(@Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        List<StockReplenishmentResponseDto> orders = vendorService.getAssignedReplenishmentsForVendor(username);
        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Assigned vendor purchase orders retrieved successfully")
                .data(orders)
                .build()).build();
    }

    @POST
    @Path("/replenishments/{replenishmentRef}/fulfill")
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "VENDOR", "WAREHOUSE_MANAGER"})
    public Response fulfillReplenishmentOrder(@PathParam("replenishmentRef") String replenishmentRef,
                                              @Context SecurityContext securityContext) {
        String username = getAuthenticatedUsername(securityContext);
        VendorFulfillmentResponseDto response = vendorService.fulfillReplenishmentOrder(replenishmentRef, username);
        return Response.ok(StandardResponseDto.builder()
                .code(Response.Status.OK.getStatusCode())
                .message("Replenishment delivery processed. Warehouse inventory updated.")
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