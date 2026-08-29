package com.globaltrade.core.entity;

import com.globaltrade.core.enums.TransportMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "route_segments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_plan_id", nullable = false)
    private RouteOptimizationPlan routePlan;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "origin_checkpoint", length = 120, nullable = false)
    private String originCheckpoint;

    @Column(name = "destination_checkpoint", length = 120, nullable = false)
    private String destinationCheckpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment_transport_mode", length = 30, nullable = false)
    private TransportMode segmentTransportMode;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "transit_hours", nullable = false)
    private Double transitHours;

    @Column(name = "segment_cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal segmentCost;
}