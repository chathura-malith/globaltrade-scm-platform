package com.globaltrade.core.dto.request;

import com.globaltrade.core.enums.VendorStatus;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorPerformanceAlertDto implements Serializable {

    private String vendorCode;
    private String vendorName;
    private String recipientEmail;
    private VendorStatus previousStatus;
    private VendorStatus currentStatus;
    private Double onTimeDeliveryRate;
    private Double customsComplianceScore;
    private Integer slaBreachCount;
    private String alertSeverity;
    private String reason;
}