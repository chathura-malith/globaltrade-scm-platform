package com.globaltrade.core.dto.response;

import com.globaltrade.core.enums.ReplenishmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReplenishmentResponseDto implements Serializable {
    private Long id;
    private String replenishmentRef;
    private String sku;
    private String itemName;
    private String warehouseCode;
    private Integer requestedQuantity;
    private ReplenishmentStatus status;
    private String triggeredBy;
    private LocalDateTime createdAt;
}