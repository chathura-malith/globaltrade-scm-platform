package com.globaltrade.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_performances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_user_id", nullable = false, unique = true)
    private User vendorUser;

    @Column(name = "vendor_name", length = 100, nullable = false)
    private String vendorName;

    @Column(name = "on_time_delivery_rate")
    private Double onTimeDeliveryRate;

    @Column(name = "customs_compliance_score")
    private Double customsComplianceScore;

    @Column(name = "sla_breach_count", nullable = false)
    @Builder.Default
    private Integer slaBreachCount = 0;

    @Column(name = "last_evaluated_at")
    private LocalDateTime lastEvaluatedAt;
}