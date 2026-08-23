package com.globaltrade.core.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCarrierSyncRequestDto implements Serializable {

    @NotBlank(message = "Carrier name is required")
    private String carrierName;

    @NotBlank(message = "Batch reference code is required")
    private String batchReference;

    @Valid
    @NotEmpty(message = "At least one checkpoint item must be provided in batch")
    private List<CarrierCheckpointSyncDto> checkpoints;
}