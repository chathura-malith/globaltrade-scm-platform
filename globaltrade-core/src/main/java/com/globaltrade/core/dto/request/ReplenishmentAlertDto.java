package com.globaltrade.core.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplenishmentAlertDto implements Serializable {
    private String replenishmentRef;
    private String sku;
    private String itemName;
    private String warehouseCode;
    private Integer requestedQuantity;
    private String triggeredBy;
}