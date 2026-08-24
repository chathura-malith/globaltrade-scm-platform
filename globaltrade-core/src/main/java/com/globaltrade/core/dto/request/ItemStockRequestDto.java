package com.globaltrade.core.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ItemStockRequestDto implements Serializable {

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotBlank(message = "Item name is required")
    private String itemName;

    @NotBlank(message = "Warehouse code is required")
    private String warehouseCode;

    @NotNull(message = "Available quantity is required")
    @Min(value = 0, message = "Available quantity cannot be negative")
    private Integer availableQuantity;

    @NotNull(message = "Reorder threshold is required")
    @Min(value = 1, message = "Reorder threshold must be at least 1")
    private Integer reorderThreshold;

    @NotNull(message = "Reorder quantity is required")
    @Min(value = 1, message = "Reorder quantity must be at least 1")
    private Integer reorderQuantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than zero")
    private BigDecimal unitPrice;
}