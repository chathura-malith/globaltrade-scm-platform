package com.globaltrade.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemStockResponseDto implements Serializable {
    private Long id;
    private String sku;
    private String itemName;
    private String warehouseCode;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer reorderThreshold;
    private Integer reorderQuantity;
    private BigDecimal unitPrice;
    private LocalDateTime updatedAt;
}