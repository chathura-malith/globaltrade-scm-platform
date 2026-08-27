package com.globaltrade.core.dto.request;

import com.globaltrade.core.enums.CustomsStatus;
import jakarta.validation.constraints.NotNull;
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
public class CustomsClearanceActionDto implements Serializable {

    @NotNull(message = "Customs decision status must be specified (CLEARED, REJECTED, HELD_FOR_SANCTION)")
    private CustomsStatus status;

    private BigDecimal additionalPenaltyAmount;

    private String inspectionNotes;
}