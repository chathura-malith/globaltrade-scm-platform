package com.globaltrade.core.dto.response;

import com.globaltrade.core.enums.ReplenishmentStatus;
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
public class VendorFulfillmentResponseDto implements Serializable {
    private String replenishmentRef;
    private String sku;
    private Integer fulfilledQuantity;
    private ReplenishmentStatus status;
    private LocalDateTime actualDeliveryDate;
    private boolean onTime;
    private String message;
}