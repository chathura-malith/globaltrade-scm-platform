package com.globaltrade.core.dto.response;

import com.globaltrade.core.enums.VendorStatus;
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
public class VendorResponseDto implements Serializable {
    private Long id;
    private String vendorCode;
    private String username;
    private String vendorName;
    private String email;
    private VendorStatus status;
    private Double onTimeDeliveryRate;
    private Double customsComplianceScore;
    private Integer totalOrdersAssigned;
    private Integer totalOrdersFulfilled;
    private Integer onTimeDeliveries;
    private Integer slaBreachCount;
    private LocalDateTime lastEvaluatedAt;
    private LocalDateTime createdAt;
}