package com.globaltrade.core.dto.response;

import com.globaltrade.core.enums.ShipmentStatus;
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
public class ShipmentCheckpointResponseDto implements Serializable {
    private Long id;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private ShipmentStatus statusAtCheckpoint;
    private String remarks;
    private String updatedByUsername;
    private LocalDateTime timestamp;
}