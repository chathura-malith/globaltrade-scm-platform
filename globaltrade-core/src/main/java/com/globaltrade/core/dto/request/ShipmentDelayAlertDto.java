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
public class ShipmentDelayAlertDto implements Serializable {
    private String trackingNumber;
    private String recipientEmail;
    private String delayReason;
    private long overdueHours;
}