package com.globaltrade.core.dto.response;

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
public class BatchCarrierSyncResponseDto implements Serializable {
    private String batchReference;
    private String carrierName;
    private int totalRecords;
    private int processedSuccessCount;
    private int failureCount;
    private List<String> errorDetails;
}