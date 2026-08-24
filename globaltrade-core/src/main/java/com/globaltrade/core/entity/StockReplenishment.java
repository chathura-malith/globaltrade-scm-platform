package com.globaltrade.core.entity;

import com.globaltrade.core.enums.ReplenishmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_replenishments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReplenishment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "replenishment_ref", length = 60, nullable = false, unique = true)
    private String replenishmentRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_stock_id", nullable = false)
    private ItemStock itemStock;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ReplenishmentStatus status;

    @Column(name = "triggered_by", length = 50, nullable = false)
    private String triggeredBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ReplenishmentStatus.PENDING;
        }
    }
}