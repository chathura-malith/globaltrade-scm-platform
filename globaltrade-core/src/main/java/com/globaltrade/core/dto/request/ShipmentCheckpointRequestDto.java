package com.globaltrade.core.dto.request;

import com.globaltrade.core.enums.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentCheckpointRequestDto implements Serializable {

    @NotBlank(message = "Location name is required")
    private String locationName;

    private Double latitude;

    private Double longitude;

    @NotNull(message = "Status at checkpoint is required")
    private ShipmentStatus status;

    private String remarks;
}