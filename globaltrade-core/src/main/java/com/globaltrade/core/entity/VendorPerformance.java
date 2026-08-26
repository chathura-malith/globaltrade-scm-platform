package com.globaltrade.core.entity;

import com.globaltrade.core.enums.VendorStatus;
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

    @Column(name = "vendor_code", length = 50, nullable = false, unique = true)
    private String vendorCode;

    @Column(name = "vendor_name", length = 150, nullable = false)
    private String vendorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private VendorStatus status = VendorStatus.ACTIVE;

    @Column(name = "on_time_delivery_rate")
    @Builder.Default
    private Double onTimeDeliveryRate = 100.0;

    @Column(name = "customs_compliance_score")
    @Builder.Default
    private Double customsComplianceScore = 100.0;

    @Column(name = "total_orders_assigned", nullable = false)
    @Builder.Default
    private Integer totalOrdersAssigned = 0;

    @Column(name = "total_orders_fulfilled", nullable = false)
    @Builder.Default
    private Integer totalOrdersFulfilled = 0;

    @Column(name = "on_time_deliveries", nullable = false)
    @Builder.Default
    private Integer onTimeDeliveries = 0;

    @Column(name = "sla_breach_count", nullable = false)
    @Builder.Default
    private Integer slaBreachCount = 0;

    @Column(name = "last_evaluated_at")
    private LocalDateTime lastEvaluatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}