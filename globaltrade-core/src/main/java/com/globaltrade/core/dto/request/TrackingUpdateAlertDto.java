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
public class TrackingUpdateAlertDto implements Serializable {
    private String trackingNumber;
    private String status;
    private Double latitude;
    private Double longitude;
    private String remarks;
}