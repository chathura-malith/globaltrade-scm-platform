package com.globaltrade.ejb.session;

import com.globaltrade.core.dto.request.AddressRequestDto;
import com.globaltrade.core.dto.request.ShipmentCheckpointRequestDto;
import com.globaltrade.core.dto.request.ShipmentRequestDto;
import com.globaltrade.core.dto.response.*;
import com.globaltrade.core.entity.Address;
import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.entity.ShipmentCheckpoint;
import com.globaltrade.core.entity.ShipmentItem;
import com.globaltrade.core.entity.User;
import com.globaltrade.core.enums.ShipmentStatus;
import com.globaltrade.core.enums.UserRole;
import com.globaltrade.core.exception.InvalidInputException;
import com.globaltrade.core.exception.InvalidShipmentStateException;
import com.globaltrade.core.exception.SecurityAuthenticationException;
import com.globaltrade.core.exception.ShipmentNotFoundException;
import com.globaltrade.core.service.ShipmentService;
import com.globaltrade.ejb.interceptor.LogisticsAuditInterceptor;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "WAREHOUSE_MANAGER", "VENDOR"})
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Interceptors(LogisticsAuditInterceptor.class)
public class ShipmentSessionBean implements ShipmentService {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    private static final String TRACKING_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR"})
    public ShipmentTrackNoResponseDto createShipment(ShipmentRequestDto request, String username) {
        if (request == null) {
            throw new InvalidInputException("Shipment request payload cannot be empty", 400);
        }

        User creator = findUserByUsername(username);
        String trackingNumber = generateUniqueTrackingNumber();

        Address origin = Address.builder()
                .street(request.getOriginAddress().getStreet())
                .city(request.getOriginAddress().getCity())
                .state(request.getOriginAddress().getState())
                .postalCode(request.getOriginAddress().getPostalCode())
                .country(request.getOriginAddress().getCountry())
                .build();

        Address destination = Address.builder()
                .street(request.getDestinationAddress().getStreet())
                .city(request.getDestinationAddress().getCity())
                .state(request.getDestinationAddress().getState())
                .postalCode(request.getDestinationAddress().getPostalCode())
                .country(request.getDestinationAddress().getCountry())
                .build();

        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .status(ShipmentStatus.CREATED)
                .transportMode(request.getTransportMode())
                .originAddress(origin)
                .destinationAddress(destination)
                .cargoWeightKg(request.getCargoWeightKg())
                .cargoVolumeCbm(request.getCargoVolumeCbm())
                .latitude(request.getInitialLatitude())
                .longitude(request.getInitialLongitude())
                .estimatedDeliveryDate(request.getEstimatedDeliveryDate())
                .createdBy(creator)
                .build();

        List<ShipmentItem> items = request.getItems().stream()
                .map(itemDto -> ShipmentItem.builder()
                        .shipment(shipment)
                        .itemName(itemDto.getItemName())
                        .hsCode(itemDto.getHsCode())
                        .quantity(itemDto.getQuantity())
                        .unitPrice(itemDto.getUnitPrice())
                        .declaredValue(itemDto.getDeclaredValue())
                        .build())
                .collect(Collectors.toList());

        shipment.setItems(items);

        em.persist(shipment);
        em.flush();

        return ShipmentTrackNoResponseDto.builder()
                .trackingNumber(trackingNumber)
                .build();
    }

    @Override
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ShipmentTrackingResponseDto getShipmentByTrackingNumber(String trackingNumber) {
        Shipment shipment = findShipmentByTrackingNumber(trackingNumber);
        return mapToTrackingResponse(shipment);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "WAREHOUSE_MANAGER"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<ShipmentTrackingResponseDto> getAllShipments(int page, int size) {
        int firstResult = Math.max(0, page) * Math.max(1, size);

        List<Shipment> shipments = em.createQuery(
                        "SELECT s FROM Shipment s ORDER BY s.createdAt DESC", Shipment.class)
                .setFirstResult(firstResult)
                .setMaxResults(size)
                .getResultList();

        for (Shipment s : shipments) {
            s.getItems().size();
            s.getCheckpoints().size();
        }

        return shipments.stream()
                .map(this::mapToTrackingResponse)
                .collect(Collectors.toList());
    }


    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "WAREHOUSE_MANAGER"})
    public ShipmentCheckpointResponseDto addCheckpoint(String trackingNumber, ShipmentCheckpointRequestDto request, String username) {
        if (request == null) {
            throw new InvalidInputException("Checkpoint request payload cannot be empty", 400);
        }

        Shipment shipment = findShipmentByTrackingNumber(trackingNumber);
        User user = findUserByUsername(username);

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new InvalidShipmentStateException("Cannot add checkpoints to a shipment that is already DELIVERED");
        }

        validateUserRoleForStatus(user.getRole(), request.getStatus());

        ShipmentCheckpoint checkpoint = ShipmentCheckpoint.builder()
                .shipment(shipment)
                .locationName(request.getLocationName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .statusAtCheckpoint(request.getStatus())
                .remarks(request.getRemarks())
                .updatedBy(user)
                .build();

        shipment.setStatus(request.getStatus());
        if (request.getLatitude() != null) {
            shipment.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            shipment.setLongitude(request.getLongitude());
        }

        if (request.getStatus() == ShipmentStatus.DELIVERED) {
            shipment.setActualDeliveryDate(LocalDateTime.now());
        }

        try {
            shipment.getCheckpoints().add(checkpoint);
            em.persist(checkpoint);
            em.merge(shipment);
            em.flush();
        } catch (OptimisticLockException e) {
            throw new InvalidShipmentStateException("Conflict: Shipment was updated by another user concurrently. Please retry.");
        }

        return mapToCheckpointResponse(checkpoint);
    }

    private void validateUserRoleForStatus(UserRole role, ShipmentStatus status) {
        if (role == UserRole.SYSTEM_ADMIN || role == UserRole.ADMIN) {
            return;
        }

        switch (role) {
            case CUSTOMS_OFFICER:
                if (status != ShipmentStatus.CUSTOMS_HOLD && status != ShipmentStatus.CLEARED) {
                    throw new InvalidShipmentStateException("Customs Officers are only authorized to set CUSTOMS_HOLD or CLEARED status", 403);
                }
                break;

            case WAREHOUSE_MANAGER:
                if (status != ShipmentStatus.WAREHOUSE_RECEIVED && status != ShipmentStatus.IN_TRANSIT) {
                    throw new InvalidShipmentStateException("Warehouse Managers can only set WAREHOUSE_RECEIVED or IN_TRANSIT status", 403);
                }
                break;

            case LOGISTICS_COORDINATOR:
                if (status != ShipmentStatus.DISPATCHED &&
                        status != ShipmentStatus.IN_TRANSIT &&
                        status != ShipmentStatus.OUT_FOR_DELIVERY &&
                        status != ShipmentStatus.DELIVERED) {
                    throw new InvalidShipmentStateException("Logistics Coordinators can only manage dispatch, transit, and delivery statuses", 403);
                }
                break;

            default:
                throw new InvalidShipmentStateException("User role is not authorized to add checkpoints", 403);
        }
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN"})
    public ShipmentTrackingResponseDto updateShipmentStatus(String trackingNumber, ShipmentStatus newStatus, String username) {
        Shipment shipment = findShipmentByTrackingNumber(trackingNumber);
        User user = findUserByUsername(username);

        if (shipment.getStatus() == ShipmentStatus.DELIVERED && newStatus != ShipmentStatus.DELIVERED) {
            throw new InvalidShipmentStateException("Delivered shipment status cannot be modified");
        }

        shipment.setStatus(newStatus);
        if (newStatus == ShipmentStatus.DELIVERED) {
            shipment.setActualDeliveryDate(LocalDateTime.now());
        }

        ShipmentCheckpoint statusCheckpoint = ShipmentCheckpoint.builder()
                .shipment(shipment)
                .locationName("System Status Update")
                .statusAtCheckpoint(newStatus)
                .remarks("Status transitioned to " + newStatus.name())
                .updatedBy(user)
                .build();

        try {
            shipment.getCheckpoints().add(statusCheckpoint);
            em.persist(statusCheckpoint);
            em.merge(shipment);
            em.flush();
        } catch (OptimisticLockException e) {
            throw new InvalidShipmentStateException("Conflict: Concurrent update detected during status transition.");
        }

        return mapToTrackingResponse(shipment);
    }

    private Shipment findShipmentByTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new InvalidInputException("Tracking number must be provided", 400);
        }
        try {
            Shipment shipment = em.createQuery(
                            "SELECT s FROM Shipment s LEFT JOIN FETCH s.items WHERE s.trackingNumber = :tn", Shipment.class)
                    .setParameter("tn", trackingNumber.trim().toUpperCase())
                    .getSingleResult();

            if (shipment != null) {
                shipment.getCheckpoints().size();
            }

            return shipment;
        } catch (NoResultException e) {
            throw new ShipmentNotFoundException("No shipment found with tracking number: " + trackingNumber);
        }
    }


    private User findUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new SecurityAuthenticationException("User identity not available in security context", 401);
        }
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :un", User.class)
                    .setParameter("un", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new SecurityAuthenticationException("Authenticated user record not found in system", 401);
        }
    }

    private String generateUniqueTrackingNumber() {
        String code;
        boolean exists;
        do {
            StringBuilder sb = new StringBuilder("GT-");
            for (int i = 0; i < 9; i++) {
                sb.append(TRACKING_CHARS.charAt(RANDOM.nextInt(TRACKING_CHARS.length())));
            }
            code = sb.toString();

            Long count = em.createQuery(
                            "SELECT COUNT(s) FROM Shipment s WHERE s.trackingNumber = :code", Long.class)
                    .setParameter("code", code)
                    .getSingleResult();
            exists = count > 0;
        } while (exists);

        return code;
    }

    private ShipmentTrackingResponseDto mapToTrackingResponse(Shipment s) {
        AddressResponseDto originDto = s.getOriginAddress() != null ? AddressResponseDto.builder()
                .street(s.getOriginAddress().getStreet())
                .city(s.getOriginAddress().getCity())
                .state(s.getOriginAddress().getState())
                .postalCode(s.getOriginAddress().getPostalCode())
                .country(s.getOriginAddress().getCountry())
                .build() : null;

        AddressResponseDto destDto = s.getDestinationAddress() != null ? AddressResponseDto.builder()
                .street(s.getDestinationAddress().getStreet())
                .city(s.getDestinationAddress().getCity())
                .state(s.getDestinationAddress().getState())
                .postalCode(s.getDestinationAddress().getPostalCode())
                .country(s.getDestinationAddress().getCountry())
                .build() : null;

        List<ShipmentItemResponseDto> itemDtos = s.getItems().stream()
                .map(item -> ShipmentItemResponseDto.builder()
                        .id(item.getId())
                        .itemName(item.getItemName())
                        .hsCode(item.getHsCode())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .declaredValue(item.getDeclaredValue())
                        .build())
                .collect(Collectors.toList());

        List<ShipmentCheckpointResponseDto> checkpointDtos = s.getCheckpoints().stream()
                .map(this::mapToCheckpointResponse)
                .collect(Collectors.toList());

        return ShipmentTrackingResponseDto.builder()
                .id(s.getId())
                .trackingNumber(s.getTrackingNumber())
                .status(s.getStatus())
                .transportMode(s.getTransportMode())
                .originAddress(originDto)
                .destinationAddress(destDto)
                .cargoWeightKg(s.getCargoWeightKg())
                .cargoVolumeCbm(s.getCargoVolumeCbm())
                .currentLatitude(s.getLatitude())
                .currentLongitude(s.getLongitude())
                .estimatedDeliveryDate(s.getEstimatedDeliveryDate())
                .actualDeliveryDate(s.getActualDeliveryDate())
                .createdByUsername(s.getCreatedBy() != null ? s.getCreatedBy().getUsername() : null)
                .createdAt(s.getCreatedAt())
                .items(itemDtos)
                .checkpoints(checkpointDtos)
                .build();
    }

    private ShipmentCheckpointResponseDto mapToCheckpointResponse(ShipmentCheckpoint cp) {
        return ShipmentCheckpointResponseDto.builder()
                .id(cp.getId())
                .locationName(cp.getLocationName())
                .latitude(cp.getLatitude())
                .longitude(cp.getLongitude())
                .statusAtCheckpoint(cp.getStatusAtCheckpoint())
                .remarks(cp.getRemarks())
                .updatedByUsername(cp.getUpdatedBy() != null ? cp.getUpdatedBy().getUsername() : null)
                .timestamp(cp.getTimestamp())
                .build();
    }
}