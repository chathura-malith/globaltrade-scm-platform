package com.globaltrade.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentItemResponseDto implements Serializable {
    private Long id;
    private String itemName;
    private String hsCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal declaredValue;
}