package com.globaltrade.core.dto.response;

import com.globaltrade.core.enums.TransportMode;
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
public class RouteSegmentResponseDto implements Serializable {

    private Integer sequenceOrder;
    private String originCheckpoint;
    private String destinationCheckpoint;
    private TransportMode transportMode;
    private Double distanceKm;
    private Double transitHours;
    private BigDecimal segmentCost;
}