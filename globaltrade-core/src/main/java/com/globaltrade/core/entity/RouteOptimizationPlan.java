package com.globaltrade.core.entity;

import com.globaltrade.core.enums.OptimizationStrategy;
import com.globaltrade.core.enums.RoutePlanStatus;
import com.globaltrade.core.enums.TransportMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "route_optimization_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteOptimizationPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "plan_reference", length = 60, nullable = false, unique = true)
    private String planReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", length = 30, nullable = false)
    private OptimizationStrategy strategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private RoutePlanStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_transport_mode", length = 30, nullable = false)
    private TransportMode recommendedTransportMode;

    @Column(name = "total_distance_km", nullable = false)
    private Double totalDistanceKm;

    @Column(name = "estimated_transit_hours", nullable = false)
    private Double estimatedTransitHours;

    @Column(name = "estimated_freight_cost", precision = 12, scale = 2, nullable = false)
    private BigDecimal estimatedFreightCost;

    @Column(name = "carbon_footprint_kg_co2")
    private Double carbonFootprintKgCo2;

    @Column(name = "delay_risk_score")
    @Builder.Default
    private Double delayRiskScore = 0.0;

    @Lob
    @Column(name = "optimization_reasoning", columnDefinition = "TEXT")
    private String optimizationReasoning;

    @OneToMany(mappedBy = "routePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<RouteSegment> segments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = RoutePlanStatus.PROPOSED;
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}