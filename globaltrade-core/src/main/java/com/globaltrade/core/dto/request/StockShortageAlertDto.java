package com.globaltrade.core.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockShortageAlertDto implements Serializable {
    private String sku;
    private String itemName;
    private String warehouseCode;
    private int currentStock;
    private int threshold;
    private String replenishmentRef;
}