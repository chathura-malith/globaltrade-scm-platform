package com.globaltrade.core.dto.response;

import com.globaltrade.core.enums.ShipmentStatus;
import com.globaltrade.core.enums.TransportMode;
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
public class ShipmentTrackingResponseDto implements Serializable {
    private Long id;
    private String trackingNumber;
    private ShipmentStatus status;
    private TransportMode transportMode;
    private AddressResponseDto originAddress;
    private AddressResponseDto destinationAddress;
    private Double cargoWeightKg;
    private Double cargoVolumeCbm;
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private List<ShipmentItemResponseDto> items;
    private List<ShipmentCheckpointResponseDto> checkpoints;
}