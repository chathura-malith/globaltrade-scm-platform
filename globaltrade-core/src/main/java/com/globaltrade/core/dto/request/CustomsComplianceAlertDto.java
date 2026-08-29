package com.globaltrade.core.dto.request;

import com.globaltrade.core.enums.CustomsStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomsComplianceAlertDto implements Serializable {

    private String declarationNumber;
    private String trackingNumber;
    private String originCountry;
    private String destinationCountry;
    private CustomsStatus previousStatus;
    private CustomsStatus currentStatus;
    private BigDecimal declaredValue;
    private BigDecimal dutyAmount;
    private String tradeAgreement;
    private String alertSeverity;
    private String complianceIssue;
}