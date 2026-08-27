package com.globaltrade.core.dto.response;

import com.globaltrade.core.enums.CustomsStatus;
import com.globaltrade.core.enums.TradeAgreementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomsDeclarationResponseDto implements Serializable {

    private Long id;
    private String declarationNumber;
    private String trackingNumber;
    private CustomsStatus customsStatus;
    private TradeAgreementType tradeAgreementType;
    private String originCountry;
    private String destinationCountry;
    private BigDecimal totalDeclaredValue;
    private Double dutyRatePercentage;
    private BigDecimal dutyAmount;
    private BigDecimal penaltyAmount;
    private boolean sanctionFlagged;
    private LocalDateTime submissionDeadline;
    private String inspectedBy;
    private LocalDateTime clearanceDate;
    private String inspectionNotes;
    private List<CustomsDocumentResponseDto> documents;
    private LocalDateTime createdAt;
}