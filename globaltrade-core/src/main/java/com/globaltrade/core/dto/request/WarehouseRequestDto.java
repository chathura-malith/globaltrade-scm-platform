package com.globaltrade.core.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseRequestDto implements Serializable {

    @NotBlank(message = "Warehouse name is required")
    private String warehouseName;

    @Valid
    @NotNull(message = "Location address is required")
    private AddressRequestDto locationAddress;

    @NotNull(message = "Capacity CBM is required")
    @Positive(message = "Capacity must be greater than zero")
    private Double capacityCbm;
}