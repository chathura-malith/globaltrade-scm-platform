package com.globaltrade.core.dto.request;

import com.globaltrade.core.enums.OptimizationStrategy;
import com.globaltrade.core.enums.TransportMode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteOptimizationRequestDto implements Serializable {

    @NotNull(message = "Optimization strategy is required (FASTEST, MOST_COST_EFFECTIVE, BALANCED, GREEN_LOGISTICS)")
    private OptimizationStrategy strategy;

    private TransportMode preferredTransportMode;

    private boolean avoidHighRiskCorridors;
}