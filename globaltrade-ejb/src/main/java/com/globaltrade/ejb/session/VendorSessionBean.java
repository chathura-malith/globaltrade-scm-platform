package com.globaltrade.ejb.session;

import com.globaltrade.core.dto.request.AssignReplenishmentVendorRequestDto;
import com.globaltrade.core.dto.request.StockAdjustmentRequestDto;
import com.globaltrade.core.dto.request.VendorPerformanceAlertDto;
import com.globaltrade.core.dto.response.StockReplenishmentResponseDto;
import com.globaltrade.core.dto.response.VendorFulfillmentResponseDto;
import com.globaltrade.core.dto.response.VendorResponseDto;
import com.globaltrade.core.entity.ItemStock;
import com.globaltrade.core.entity.StockReplenishment;
import com.globaltrade.core.entity.User;
import com.globaltrade.core.entity.VendorPerformance;
import com.globaltrade.core.enums.ReplenishmentStatus;
import com.globaltrade.core.enums.UserRole;
import com.globaltrade.core.enums.VendorStatus;
import com.globaltrade.core.exception.*;
import com.globaltrade.core.service.InventoryService;
import com.globaltrade.core.service.NotificationService;
import com.globaltrade.core.service.VendorService;
import com.globaltrade.ejb.interceptor.LogisticsAuditInterceptor;
import com.globaltrade.ejb.interceptor.VendorValidationInterceptor;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.*;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "VENDOR"})
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Interceptors({LogisticsAuditInterceptor.class, VendorValidationInterceptor.class})
public class VendorSessionBean implements VendorService {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private InventoryService inventoryService;

    @EJB
    private NotificationService notificationService;

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public VendorResponseDto getVendorProfileByCode(String vendorCode) {
        VendorPerformance vendor = findVendorByCode(vendorCode);
        return mapToResponse(vendor);
    }

    @Override
    @RolesAllowed({"VENDOR"})
    public VendorResponseDto getVendorProfileByUsername(String username) {
        VendorPerformance vendor = findVendorByUsername(username);
        return mapToResponse(vendor);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public List<VendorResponseDto> getAllVendors() {
        List<VendorPerformance> vendors = em.createQuery(
                        "SELECT vp FROM VendorPerformance vp ORDER BY vp.onTimeDeliveryRate DESC", VendorPerformance.class)
                .getResultList();

        return vendors.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN"})
    public VendorResponseDto updateVendorStatus(String vendorCode, VendorStatus newStatus, String username) {
        if (newStatus == null) {
            throw new InvalidInputException("New vendor status must be specified", 400);
        }

        VendorPerformance vendor = findVendorByCode(vendorCode);
        vendor.setStatus(newStatus);
        vendor.setUpdatedAt(LocalDateTime.now());

        try {
            em.merge(vendor);
            em.flush();
        } catch (OptimisticLockException e) {
            throw new InvalidVendorStateException("Conflict: Vendor record was concurrently modified. Please retry.", 409);
        }

        return mapToResponse(vendor);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public StockReplenishmentResponseDto assignReplenishmentOrder(AssignReplenishmentVendorRequestDto request, String username) {
        if (request == null) {
            throw new InvalidInputException("Assignment request payload cannot be empty", 400);
        }

        StockReplenishment replenishment = findReplenishmentByRef(request.getReplenishmentRef());

        if (replenishment.getStatus() == ReplenishmentStatus.COMPLETED || replenishment.getStatus() == ReplenishmentStatus.CANCELLED) {
            throw new InvalidVendorStateException("Cannot assign a replenishment order that is already " + replenishment.getStatus(), 409);
        }

        VendorPerformance vendor = findVendorByIdentifier(request.getVendorIdentifier());
        User vendorUser = vendor.getVendorUser();

        replenishment.setAssignedVendor(vendorUser);
        replenishment.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        replenishment.setStatus(ReplenishmentStatus.ORDERED);

        vendor.setTotalOrdersAssigned(vendor.getTotalOrdersAssigned() + 1);

        try {
            em.merge(replenishment);
            em.merge(vendor);
            em.flush();
        } catch (OptimisticLockException e) {
            throw new InvalidVendorStateException("Conflict: Concurrent update detected during replenishment assignment.", 409);
        }

        return mapToReplenishmentResponse(replenishment);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "VENDOR", "WAREHOUSE_MANAGER"})
    public VendorFulfillmentResponseDto fulfillReplenishmentOrder(String replenishmentRef, String username) {
        StockReplenishment replenishment = findReplenishmentByRef(replenishmentRef);

        if (replenishment.getStatus() == ReplenishmentStatus.COMPLETED) {
            throw new InvalidVendorStateException("Replenishment order [" + replenishmentRef + "] has already been fulfilled and completed", 409);
        }

        if (replenishment.getStatus() == ReplenishmentStatus.CANCELLED) {
            throw new InvalidVendorStateException("Cannot fulfill a CANCELLED replenishment order", 409);
        }

        User caller = findUserByUsername(username);

        if (caller.getRole() == UserRole.VENDOR) {
            if (replenishment.getAssignedVendor() == null || !replenishment.getAssignedVendor().getId().equals(caller.getId())) {
                throw new SecurityAuthenticationException("Unauthorized: You are only allowed to fulfill orders assigned to your organization", 403);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        replenishment.setActualDeliveryDate(now);
        replenishment.setStatus(ReplenishmentStatus.COMPLETED);

        boolean isOnTime = true;
        if (replenishment.getEstimatedDeliveryDate() != null && now.isAfter(replenishment.getEstimatedDeliveryDate())) {
            isOnTime = false;
        }

        if (replenishment.getAssignedVendor() != null) {
            VendorPerformance vendor = findVendorByUserId(replenishment.getAssignedVendor().getId());
            if (vendor != null) {
                vendor.setTotalOrdersFulfilled(vendor.getTotalOrdersFulfilled() + 1);
                if (isOnTime) {
                    vendor.setOnTimeDeliveries(vendor.getOnTimeDeliveries() + 1);
                } else {
                    vendor.setSlaBreachCount(vendor.getSlaBreachCount() + 1);
                }

                if (vendor.getTotalOrdersFulfilled() > 0) {
                    double rate = ((double) vendor.getOnTimeDeliveries() / vendor.getTotalOrdersFulfilled()) * 100.0;
                    vendor.setOnTimeDeliveryRate(Math.round(rate * 100.0) / 100.0);
                }
                em.merge(vendor);
            }
        }

        ItemStock itemStock = replenishment.getItemStock();
        StockAdjustmentRequestDto adjustDto = StockAdjustmentRequestDto.builder()
                .sku(itemStock.getSku())
                .quantityChange(replenishment.getRequestedQuantity())
                .reason("Vendor Replenishment Fulfillment Inbound: Ref [" + replenishmentRef + "]")
                .build();

        inventoryService.adjustStock(adjustDto, username);

        try {
            em.merge(replenishment);
            em.flush();
        } catch (OptimisticLockException e) {
            throw new InvalidVendorStateException("Conflict: Concurrently updated during order fulfillment.", 409);
        }

        return VendorFulfillmentResponseDto.builder()
                .replenishmentRef(replenishmentRef)
                .sku(itemStock.getSku())
                .fulfilledQuantity(replenishment.getRequestedQuantity())
                .status(ReplenishmentStatus.COMPLETED)
                .actualDeliveryDate(now)
                .onTime(isOnTime)
                .message("Replenishment delivery processed. Warehouse stock replenished successfully.")
                .build();
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "VENDOR"})
    public List<StockReplenishmentResponseDto> getAssignedReplenishmentsForVendor(String username) {
        User vendorUser = findUserByUsername(username);

        List<StockReplenishment> orders = em.createQuery(
                        "SELECT r FROM StockReplenishment r WHERE r.assignedVendor.id = :vid ORDER BY r.createdAt DESC",
                        StockReplenishment.class)
                .setParameter("vid", vendorUser.getId())
                .getResultList();

        return orders.stream()
                .map(this::mapToReplenishmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void evaluateAllVendorPerformances() {
        List<VendorPerformance> allVendors = em.createQuery(
                        "SELECT vp FROM VendorPerformance vp", VendorPerformance.class)
                .getResultList();

        LocalDateTime now = LocalDateTime.now();

        for (VendorPerformance vendor : allVendors) {
            if (vendor.getStatus() == VendorStatus.INACTIVE) {
                continue;
            }

            VendorStatus previousStatus = vendor.getStatus();
            Long vendorUserId = vendor.getVendorUser() != null ? vendor.getVendorUser().getId() : null;

            if (vendorUserId == null) {
                continue;
            }

            List<StockReplenishment> overdueOrders = em.createQuery(
                            "SELECT r FROM StockReplenishment r WHERE r.assignedVendor.id = :vid " +
                                    "AND r.status = :status AND r.estimatedDeliveryDate < :now", StockReplenishment.class)
                    .setParameter("vid", vendorUserId)
                    .setParameter("status", ReplenishmentStatus.ORDERED)
                    .setParameter("now", now)
                    .getResultList();

            int activeOverdueCount = overdueOrders.size();

            if (vendor.getTotalOrdersFulfilled() > 0) {
                double calculatedRate = ((double) vendor.getOnTimeDeliveries() / vendor.getTotalOrdersFulfilled()) * 100.0;
                vendor.setOnTimeDeliveryRate(Math.round(calculatedRate * 100.0) / 100.0);
            }

            String alertSeverity = null;
            String alertReason = null;
            VendorStatus nextStatus = previousStatus;
            int totalBreaches = vendor.getSlaBreachCount() + activeOverdueCount;

            if (vendor.getOnTimeDeliveryRate() < 60.0 || totalBreaches >= 3) {
                nextStatus = VendorStatus.SUSPENDED;
                alertSeverity = "CRITICAL";
                alertReason = "Critical Performance SLA Breach: Delivery rate (" + vendor.getOnTimeDeliveryRate() +
                        "%) < 60% OR Total SLA Breaches (" + totalBreaches + ") >= 3.";
            }

            else if (vendor.getCustomsComplianceScore() < 70.0 || vendor.getOnTimeDeliveryRate() < 80.0 || activeOverdueCount > 0) {
                if (previousStatus != VendorStatus.SUSPENDED) {
                    nextStatus = VendorStatus.UNDER_REVIEW;
                    alertSeverity = "ESCALATION";
                    alertReason = "Compliance/Delivery Latency: Score (" + vendor.getCustomsComplianceScore() +
                            ") < 70 OR Active Overdue Shipments (" + activeOverdueCount + ").";
                }
            }
            else if (vendor.getOnTimeDeliveryRate() >= 80.0 && vendor.getCustomsComplianceScore() >= 70.0 && activeOverdueCount == 0) {
                if (previousStatus == VendorStatus.UNDER_REVIEW) {
                    nextStatus = VendorStatus.ACTIVE;
                    alertSeverity = "INFO";
                    alertReason = "Performance metrics healthy. Status restored to ACTIVE.";
                }
            }

            vendor.setStatus(nextStatus);
            vendor.setLastEvaluatedAt(now);
            em.merge(vendor);

            if (previousStatus != nextStatus || alertSeverity != null) {
                notificationService.sendVendorPerformanceAlert(
                        VendorPerformanceAlertDto.builder()
                                .vendorCode(vendor.getVendorCode())
                                .vendorName(vendor.getVendorName())
                                .recipientEmail(vendor.getVendorUser() != null ? vendor.getVendorUser().getEmail() : "ops@globaltrade.com")
                                .previousStatus(previousStatus)
                                .currentStatus(nextStatus)
                                .onTimeDeliveryRate(vendor.getOnTimeDeliveryRate())
                                .customsComplianceScore(vendor.getCustomsComplianceScore())
                                .slaBreachCount(totalBreaches)
                                .alertSeverity(alertSeverity != null ? alertSeverity : "WARNING")
                                .reason(alertReason)
                                .build()
                );
            }
        }
    }

    private VendorPerformance findVendorByCode(String vendorCode) {
        if (vendorCode == null || vendorCode.isBlank()) {
            throw new InvalidInputException("Vendor code is required", 400);
        }
        try {
            return em.createQuery("SELECT vp FROM VendorPerformance vp WHERE vp.vendorCode = :code", VendorPerformance.class)
                    .setParameter("code", vendorCode.trim().toUpperCase())
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new VendorNotFoundException("Vendor record not found with code: " + vendorCode, 404);
        }
    }

    private VendorPerformance findVendorByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new InvalidInputException("Vendor username is required", 400);
        }
        try {
            return em.createQuery("SELECT vp FROM VendorPerformance vp WHERE vp.vendorUser.username = :un", VendorPerformance.class)
                    .setParameter("un", username.trim())
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new VendorNotFoundException("Vendor record not found with username: " + username, 404);
        }
    }

    private VendorPerformance findVendorByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidInputException("Vendor identifier is required", 400);
        }
        try {
            return em.createQuery(
                            "SELECT vp FROM VendorPerformance vp WHERE vp.vendorCode = :ident OR vp.vendorUser.username = :ident",
                            VendorPerformance.class)
                    .setParameter("ident", identifier.trim())
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new VendorNotFoundException("Vendor record not found for: " + identifier, 404);
        }
    }

    private VendorPerformance findVendorByUserId(Long userId) {
        try {
            return em.createQuery("SELECT vp FROM VendorPerformance vp WHERE vp.vendorUser.id = :uid", VendorPerformance.class)
                    .setParameter("uid", userId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private StockReplenishment findReplenishmentByRef(String ref) {
        if (ref == null || ref.isBlank()) {
            throw new InvalidInputException("Replenishment reference must be provided", 400);
        }
        try {
            return em.createQuery("SELECT r FROM StockReplenishment r WHERE r.replenishmentRef = :ref", StockReplenishment.class)
                    .setParameter("ref", ref.trim().toUpperCase())
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new ResourceNotFoundException("Replenishment order not found with reference: " + ref);
        }
    }

    private User findUserByUsername(String username) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :un", User.class)
                    .setParameter("un", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new SecurityAuthenticationException("User identity record not found in system", 401);
        }
    }

    private VendorResponseDto mapToResponse(VendorPerformance v) {
        return VendorResponseDto.builder()
                .id(v.getId())
                .vendorCode(v.getVendorCode())
                .username(v.getVendorUser() != null ? v.getVendorUser().getUsername() : null)
                .vendorName(v.getVendorName())
                .email(v.getVendorUser() != null ? v.getVendorUser().getEmail() : null)
                .status(v.getStatus())
                .onTimeDeliveryRate(v.getOnTimeDeliveryRate())
                .customsComplianceScore(v.getCustomsComplianceScore())
                .totalOrdersAssigned(v.getTotalOrdersAssigned())
                .totalOrdersFulfilled(v.getTotalOrdersFulfilled())
                .onTimeDeliveries(v.getOnTimeDeliveries())
                .slaBreachCount(v.getSlaBreachCount())
                .lastEvaluatedAt(v.getLastEvaluatedAt())
                .createdAt(v.getCreatedAt())
                .build();
    }

    private StockReplenishmentResponseDto mapToReplenishmentResponse(StockReplenishment r) {
        return StockReplenishmentResponseDto.builder()
                .id(r.getId())
                .replenishmentRef(r.getReplenishmentRef())
                .sku(r.getItemStock() != null ? r.getItemStock().getSku() : null)
                .itemName(r.getItemStock() != null ? r.getItemStock().getItemName() : null)
                .warehouseCode(r.getItemStock() != null ? r.getItemStock().getWarehouseCode() : null)
                .requestedQuantity(r.getRequestedQuantity())
                .status(r.getStatus())
                .triggeredBy(r.getTriggeredBy())
                .createdAt(r.getCreatedAt())
                .build();
    }
}