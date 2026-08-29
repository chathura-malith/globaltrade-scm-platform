package com.globaltrade.core.dto.request;

import com.globaltrade.core.enums.OptimizationStrategy;
import com.globaltrade.core.enums.RoutePlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteComplianceAlertDto implements Serializable {

    private String planReference;
    private String trackingNumber;
    private String originCity;
    private String destinationCity;
    private OptimizationStrategy strategy;
    private RoutePlanStatus status;
    private Double totalDistanceKm;
    private Double estimatedHours;
    private BigDecimal estimatedCost;
    private String alertSeverity;
    private String details;
}