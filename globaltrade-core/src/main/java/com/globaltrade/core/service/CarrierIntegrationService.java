package com.globaltrade.core.service;

import com.globaltrade.core.dto.request.BatchCarrierSyncRequestDto;
import com.globaltrade.core.dto.response.BatchCarrierSyncResponseDto;
import jakarta.ejb.Local;

@Local
public interface CarrierIntegrationService {
    BatchCarrierSyncResponseDto processCarrierBatchSync(BatchCarrierSyncRequestDto request, String authenticatedUser);
}