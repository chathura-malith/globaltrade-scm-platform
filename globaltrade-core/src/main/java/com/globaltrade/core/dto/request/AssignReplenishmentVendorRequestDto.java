package com.globaltrade.core.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class AssignReplenishmentVendorRequestDto implements Serializable {

    @NotBlank(message = "Replenishment reference code is required")
    private String replenishmentRef;

    @NotBlank(message = "Vendor code or username is required")
    private String vendorIdentifier;

    @NotNull(message = "Estimated delivery date is required")
    @Future(message = "Estimated delivery date must be in the future")
    private LocalDateTime estimatedDeliveryDate;
}