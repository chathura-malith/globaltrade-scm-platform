package com.globaltrade.core.dto.request;

import com.globaltrade.core.enums.TransportMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentRequestDto implements Serializable {

    @NotNull(message = "Transport mode must be specified")
    private TransportMode transportMode;

    @Valid
    @NotNull(message = "Origin address is required")
    private AddressRequestDto originAddress;

    @Valid
    @NotNull(message = "Destination address is required")
    private AddressRequestDto destinationAddress;

    @NotNull(message = "Cargo weight is required")
    @Positive(message = "Cargo weight must be positive")
    private Double cargoWeightKg;

    @NotNull(message = "Cargo volume is required")
    @Positive(message = "Cargo volume must be positive")
    private Double cargoVolumeCbm;

    private Double initialLatitude;

    private Double initialLongitude;

    @NotNull(message = "Estimated delivery date is required")
    private LocalDateTime estimatedDeliveryDate;

    @Valid
    @NotEmpty(message = "At least one shipment item must be declared")
    private List<ShipmentItemRequestDto> items;
}