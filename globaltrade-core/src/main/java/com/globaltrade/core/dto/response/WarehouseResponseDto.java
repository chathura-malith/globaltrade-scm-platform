package com.globaltrade.core.dto.response;

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
public class WarehouseResponseDto implements Serializable {
    private Long id;
    private String warehouseCode;
    private String warehouseName;
    private AddressResponseDto locationAddress;
    private Double capacityCbm;
    private boolean active;
    private LocalDateTime createdAt;
}