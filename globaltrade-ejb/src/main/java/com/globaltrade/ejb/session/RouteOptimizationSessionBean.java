package com.globaltrade.ejb.session;

import com.globaltrade.core.dto.request.RouteComplianceAlertDto;
import com.globaltrade.core.dto.request.RouteOptimizationRequestDto;
import com.globaltrade.core.dto.response.RouteOptimizationResponseDto;
import com.globaltrade.core.dto.response.RouteSegmentResponseDto;
import com.globaltrade.core.entity.*;
import com.globaltrade.core.enums.OptimizationStrategy;
import com.globaltrade.core.enums.RoutePlanStatus;
import com.globaltrade.core.enums.ShipmentStatus;
import com.globaltrade.core.enums.TransportMode;
import com.globaltrade.core.exception.*;
import com.globaltrade.core.service.NotificationService;
import com.globaltrade.core.service.RouteOptimizationService;
import com.globaltrade.ejb.interceptor.LogisticsAuditInterceptor;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Stateless
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Interceptors({LogisticsAuditInterceptor.class})
public class RouteOptimizationSessionBean implements RouteOptimizationService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private NotificationService notificationService;

    @Override
    @PermitAll
    public RouteOptimizationResponseDto calculateAndCreateOptimizedRoute(Long shipmentId, RouteOptimizationRequestDto request, String username) {
        if (shipmentId == null) {
            throw new InvalidInputException("Shipment ID must be provided for route optimization", 400);
        }
        if (request == null || request.getStrategy() == null) {
            throw new InvalidInputException("Optimization strategy is required", 400);
        }

        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
        }

        double originLat = shipment.getLatitude() != null ? shipment.getLatitude() : 6.9271;
        double originLon = shipment.getLongitude() != null ? shipment.getLongitude() : 79.8612;

        double destLat = 52.5200;
        double destLon = 13.4050;

        double baseDistanceKm = calculateHaversineDistance(originLat, originLon, destLat, destLon);
        if (baseDistanceKm <= 0.0) {
            baseDistanceKm = 8500.0;
        }

        TransportMode selectedMode = resolveOptimalTransportMode(request.getStrategy(), request.getPreferredTransportMode(), shipment);

        double weight = shipment.getCargoWeightKg() != null ? shipment.getCargoWeightKg() : 500.0;
        double speedKmh = getAverageSpeedKmH(selectedMode);
        double costPerKmPerKg = getCostRatePerKmKg(selectedMode);
        double co2PerKmKg = getCarbonEmissionRate(selectedMode);

        double transitHours = Math.round((baseDistanceKm / speedKmh) * 10.0) / 10.0;
        BigDecimal estimatedCost = BigDecimal.valueOf(baseDistanceKm * (weight / 1000.0) * costPerKmPerKg * 100.0)
                .setScale(2, RoundingMode.HALF_UP);
        double carbonFootprint = Math.round((baseDistanceKm * (weight / 1000.0) * co2PerKmKg) * 100.0) / 100.0;
        double riskScore = calculateDelayRiskScore(selectedMode, request.isAvoidHighRiskCorridors());

        String planRef = "PLAN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String reasoning = String.format("Route optimized using %s strategy. Selected %s for %s km transit with estimated %s hours.",
                request.getStrategy().name(), selectedMode.name(), baseDistanceKm, transitHours);

        RouteOptimizationPlan plan = RouteOptimizationPlan.builder()
                .planReference(planRef)
                .shipment(shipment)
                .strategy(request.getStrategy())
                .status(RoutePlanStatus.PROPOSED)
                .recommendedTransportMode(selectedMode)
                .totalDistanceKm(baseDistanceKm)
                .estimatedTransitHours(transitHours)
                .estimatedFreightCost(estimatedCost)
                .carbonFootprintKgCo2(carbonFootprint)
                .delayRiskScore(riskScore)
                .optimizationReasoning(reasoning)
                .build();

        em.persist(plan);

        generateRouteSegments(plan, shipment, selectedMode, baseDistanceKm, transitHours, estimatedCost);

        em.flush();

        notificationService.sendRouteComplianceAlert(
                RouteComplianceAlertDto.builder()
                        .planReference(planRef)
                        .trackingNumber(shipment.getTrackingNumber())
                        .originCity(shipment.getOriginAddress() != null ? shipment.getOriginAddress().getCity() : "Origin")
                        .destinationCity(shipment.getDestinationAddress() != null ? shipment.getDestinationAddress().getCity() : "Destination")
                        .strategy(request.getStrategy())
                        .status(RoutePlanStatus.PROPOSED)
                        .totalDistanceKm(baseDistanceKm)
                        .estimatedHours(transitHours)
                        .estimatedCost(estimatedCost)
                        .alertSeverity("INFO")
                        .details("New optimized logistics routing plan generated and proposed.")
                        .build()
        );

        return mapToResponse(plan);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public RouteOptimizationResponseDto getPlanByReference(String planReference) {
        if (planReference == null || planReference.isBlank()) {
            throw new InvalidInputException("Plan reference is required", 400);
        }
        try {
            RouteOptimizationPlan plan = em.createQuery(
                            "SELECT r FROM RouteOptimizationPlan r WHERE r.planReference = :ref", RouteOptimizationPlan.class)
                    .setParameter("ref", planReference.trim().toUpperCase())
                    .getSingleResult();
            return mapToResponse(plan);
        } catch (NoResultException e) {
            throw new RouteOptimizationNotFoundException("Route optimization plan not found: " + planReference, 404);
        }
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public List<RouteOptimizationResponseDto> getPlansByShipmentTracking(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new InvalidInputException("Shipment tracking number is required", 400);
        }
        List<RouteOptimizationPlan> plans = em.createQuery(
                        "SELECT r FROM RouteOptimizationPlan r WHERE r.shipment.trackingNumber = :track ORDER BY r.createdAt DESC", RouteOptimizationPlan.class)
                .setParameter("track", trackingNumber.trim().toUpperCase())
                .getResultList();

        if (plans.isEmpty()) {
            throw new RouteOptimizationNotFoundException("No route plans found for shipment tracking: " + trackingNumber, 404);
        }

        return plans.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public List<RouteOptimizationResponseDto> getAllRoutePlans() {
        return em.createQuery("SELECT r FROM RouteOptimizationPlan r ORDER BY r.createdAt DESC", RouteOptimizationPlan.class)
                .getResultList()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR"})
    public RouteOptimizationResponseDto activateRoutePlan(String planReference, String username) {
        RouteOptimizationPlan targetPlan = findPlanByReference(planReference);

        if (targetPlan.getStatus() == RoutePlanStatus.ACTIVE) {
            throw new InvalidRouteStateException("Route plan [" + planReference + "] is already ACTIVE", 409);
        }

        List<RouteOptimizationPlan> activePlans = em.createQuery(
                        "SELECT r FROM RouteOptimizationPlan r WHERE r.shipment.id = :sid AND r.status = :st", RouteOptimizationPlan.class)
                .setParameter("sid", targetPlan.getShipment().getId())
                .setParameter("st", RoutePlanStatus.ACTIVE)
                .getResultList();

        for (RouteOptimizationPlan active : activePlans) {
            active.setStatus(RoutePlanStatus.SUPERSEDED);
            active.setUpdatedAt(LocalDateTime.now());
            em.merge(active);
        }

        targetPlan.setStatus(RoutePlanStatus.ACTIVE);
        targetPlan.setUpdatedAt(LocalDateTime.now());

        Shipment shipment = targetPlan.getShipment();
        if (shipment != null) {
            shipment.setTransportMode(targetPlan.getRecommendedTransportMode());
            shipment.setEstimatedDeliveryDate(LocalDateTime.now().plusHours(targetPlan.getEstimatedTransitHours().longValue()));
            em.merge(shipment);
        }

        try {
            em.merge(targetPlan);
            em.flush();
        } catch (OptimisticLockException e) {
            throw new InvalidRouteStateException("Concurrent modification detected while activating route plan. Retry.", 409);
        }

        notificationService.sendRouteComplianceAlert(
                RouteComplianceAlertDto.builder()
                        .planReference(targetPlan.getPlanReference())
                        .trackingNumber(shipment != null ? shipment.getTrackingNumber() : "N/A")
                        .originCity(shipment != null && shipment.getOriginAddress() != null ? shipment.getOriginAddress().getCity() : "N/A")
                        .destinationCity(shipment != null && shipment.getDestinationAddress() != null ? shipment.getDestinationAddress().getCity() : "N/A")
                        .strategy(targetPlan.getStrategy())
                        .status(RoutePlanStatus.ACTIVE)
                        .totalDistanceKm(targetPlan.getTotalDistanceKm())
                        .estimatedHours(targetPlan.getEstimatedTransitHours())
                        .estimatedCost(targetPlan.getEstimatedFreightCost())
                        .alertSeverity("INFO")
                        .details("Route plan officially activated. Carrier dispatched along optimized corridor.")
                        .build()
        );

        return mapToResponse(targetPlan);
    }

    @Override
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void evaluateAndReOptimizeActiveShipments() {
        List<Shipment> unroutedShipments = em.createQuery(
                        "SELECT s FROM Shipment s WHERE s.status IN (:statuses) AND NOT EXISTS (" +
                                " SELECT r FROM RouteOptimizationPlan r WHERE r.shipment.id = s.id" +
                                ")", Shipment.class)
                .setParameter("statuses", Arrays.asList(ShipmentStatus.CREATED, ShipmentStatus.DISPATCHED))
                .getResultList();

        for (Shipment s : unroutedShipments) {
            try {
                calculateAndCreateOptimizedRoute(
                        s.getId(),
                        RouteOptimizationRequestDto.builder().strategy(OptimizationStrategy.BALANCED).build(),
                        "SYSTEM_SCHEDULER"
                );
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(RouteOptimizationSessionBean.class.getName())
                        .log(java.util.logging.Level.SEVERE, "Auto Route Optimization Error: " + e.getMessage(), e);
            }
        }
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 100.0) / 100.0;
    }

    private TransportMode resolveOptimalTransportMode(OptimizationStrategy strategy, TransportMode preferred, Shipment shipment) {
        if (preferred != null) {
            return preferred;
        }
        if (strategy == OptimizationStrategy.FASTEST) {
            return TransportMode.AIR_CARGO;
        }
        if (strategy == OptimizationStrategy.MOST_COST_EFFECTIVE) {
            return (shipment.getCargoWeightKg() != null && shipment.getCargoWeightKg() > 2000.0)
                    ? TransportMode.OCEAN_FREIGHT : TransportMode.RAIL_FREIGHT;
        }
        if (strategy == OptimizationStrategy.GREEN_LOGISTICS) {
            return TransportMode.RAIL_FREIGHT;
        }
        return TransportMode.ROAD_FREIGHT;
    }

    private double getAverageSpeedKmH(TransportMode mode) {
        return switch (mode) {
            case AIR_CARGO -> 750.0;
            case ROAD_FREIGHT -> 70.0;
            case RAIL_FREIGHT -> 55.0;
            case OCEAN_FREIGHT -> 35.0;
        };
    }

    private double getCostRatePerKmKg(TransportMode mode) {
        return switch (mode) {
            case AIR_CARGO -> 0.0085;
            case ROAD_FREIGHT -> 0.0035;
            case RAIL_FREIGHT -> 0.0022;
            case OCEAN_FREIGHT -> 0.0012;
        };
    }

    private double getCarbonEmissionRate(TransportMode mode) {
        return switch (mode) {
            case AIR_CARGO -> 0.50;
            case ROAD_FREIGHT -> 0.12;
            case RAIL_FREIGHT -> 0.03;
            case OCEAN_FREIGHT -> 0.015;
        };
    }

    private double calculateDelayRiskScore(TransportMode mode, boolean avoidRisk) {
        double baseRisk = switch (mode) {
            case AIR_CARGO -> 8.5;
            case ROAD_FREIGHT -> 22.0;
            case RAIL_FREIGHT -> 14.0;
            case OCEAN_FREIGHT -> 32.0;
        };
        return avoidRisk ? baseRisk * 0.6 : baseRisk;
    }

    private void generateRouteSegments(RouteOptimizationPlan plan, Shipment shipment, TransportMode mode, double totalDist, double totalHours, BigDecimal totalCost) {
        if (plan.getSegments() == null) {
            plan.setSegments(new ArrayList<>());
        }

        String originCity = shipment.getOriginAddress() != null && shipment.getOriginAddress().getCity() != null ? shipment.getOriginAddress().getCity() : "Origin Terminal";
        String destCity = shipment.getDestinationAddress() != null && shipment.getDestinationAddress().getCity() != null ? shipment.getDestinationAddress().getCity() : "Destination Terminal";

        // Segment 1: Origin to Hub
        RouteSegment seg1 = RouteSegment.builder()
                .routePlan(plan)
                .sequenceOrder(1)
                .originCheckpoint(originCity + " Pickup Depot")
                .destinationCheckpoint(originCity + " International Gateway Hub")
                .segmentTransportMode(TransportMode.ROAD_FREIGHT)
                .distanceKm(Math.round(totalDist * 0.10 * 100.0) / 100.0)
                .transitHours(Math.round(totalHours * 0.15 * 10.0) / 10.0)
                .segmentCost(totalCost.multiply(BigDecimal.valueOf(0.15)).setScale(2, RoundingMode.HALF_UP))
                .build();
        em.persist(seg1);
        plan.getSegments().add(seg1);


        RouteSegment seg2 = RouteSegment.builder()
                .routePlan(plan)
                .sequenceOrder(2)
                .originCheckpoint(originCity + " International Gateway Hub")
                .destinationCheckpoint(destCity + " Central Logistics Center")
                .segmentTransportMode(mode)
                .distanceKm(Math.round(totalDist * 0.80 * 100.0) / 100.0)
                .transitHours(Math.round(totalHours * 0.70 * 10.0) / 10.0)
                .segmentCost(totalCost.multiply(BigDecimal.valueOf(0.70)).setScale(2, RoundingMode.HALF_UP))
                .build();
        em.persist(seg2);
        plan.getSegments().add(seg2);

        RouteSegment seg3 = RouteSegment.builder()
                .routePlan(plan)
                .sequenceOrder(3)
                .originCheckpoint(destCity + " Central Logistics Center")
                .destinationCheckpoint(destCity + " Final Consignee Doorstep")
                .segmentTransportMode(TransportMode.ROAD_FREIGHT)
                .distanceKm(Math.round(totalDist * 0.10 * 100.0) / 100.0)
                .transitHours(Math.round(totalHours * 0.15 * 10.0) / 10.0)
                .segmentCost(totalCost.multiply(BigDecimal.valueOf(0.15)).setScale(2, RoundingMode.HALF_UP))
                .build();
        em.persist(seg3);
        plan.getSegments().add(seg3);
    }

    private RouteOptimizationPlan findPlanByReference(String ref) {
        try {
            return em.createQuery("SELECT r FROM RouteOptimizationPlan r WHERE r.planReference = :ref", RouteOptimizationPlan.class)
                    .setParameter("ref", ref.trim().toUpperCase())
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new RouteOptimizationNotFoundException("Route plan reference not found: " + ref, 404);
        }
    }

    private RouteOptimizationResponseDto mapToResponse(RouteOptimizationPlan p) {
        List<RouteSegmentResponseDto> segDtos = p.getSegments() != null ? p.getSegments().stream()
                .map(s -> RouteSegmentResponseDto.builder()
                        .sequenceOrder(s.getSequenceOrder())
                        .originCheckpoint(s.getOriginCheckpoint())
                        .destinationCheckpoint(s.getDestinationCheckpoint())
                        .transportMode(s.getSegmentTransportMode())
                        .distanceKm(s.getDistanceKm())
                        .transitHours(s.getTransitHours())
                        .segmentCost(s.getSegmentCost())
                        .build())
                .collect(Collectors.toList()) : Collections.emptyList();

        return RouteOptimizationResponseDto.builder()
                .id(p.getId())
                .planReference(p.getPlanReference())
                .trackingNumber(p.getShipment() != null ? p.getShipment().getTrackingNumber() : null)
                .strategy(p.getStrategy())
                .status(p.getStatus())
                .recommendedTransportMode(p.getRecommendedTransportMode())
                .totalDistanceKm(p.getTotalDistanceKm())
                .estimatedTransitHours(p.getEstimatedTransitHours())
                .estimatedFreightCost(p.getEstimatedFreightCost())
                .carbonFootprintKgCo2(p.getCarbonFootprintKgCo2())
                .delayRiskScore(p.getDelayRiskScore())
                .optimizationReasoning(p.getOptimizationReasoning())
                .segments(segDtos)
                .createdAt(p.getCreatedAt())
                .build();
    }
}