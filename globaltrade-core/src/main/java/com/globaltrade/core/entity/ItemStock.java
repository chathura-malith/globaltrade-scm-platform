package com.globaltrade.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "item_stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "sku", length = 50, nullable = false, unique = true)
    private String sku;

    @Column(name = "item_name", length = 150, nullable = false)
    private String itemName;

    @Column(name = "warehouse_code", length = 50, nullable = false)
    private String warehouseCode;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "reorder_threshold", nullable = false)
    @Builder.Default
    private Integer reorderThreshold = 10;

    @Version
    @Column(name = "version")
    private Long version;
}