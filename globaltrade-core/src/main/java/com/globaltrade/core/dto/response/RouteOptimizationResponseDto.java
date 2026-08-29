package com.globaltrade.core.dto.response;

import com.globaltrade.core.enums.OptimizationStrategy;
import com.globaltrade.core.enums.RoutePlanStatus;
import com.globaltrade.core.enums.TransportMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteOptimizationResponseDto implements Serializable {

    private Long id;
    private String planReference;
    private String trackingNumber;
    private OptimizationStrategy strategy;
    private RoutePlanStatus status;
    private TransportMode recommendedTransportMode;
    private Double totalDistanceKm;
    private Double estimatedTransitHours;
    private BigDecimal estimatedFreightCost;
    private Double carbonFootprintKgCo2;
    private Double delayRiskScore;
    private String optimizationReasoning;
    private List<RouteSegmentResponseDto> segments;
    private LocalDateTime createdAt;
}