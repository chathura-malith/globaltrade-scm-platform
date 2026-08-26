package com.globaltrade.core.service;

import com.globaltrade.core.dto.request.AssignReplenishmentVendorRequestDto;
import com.globaltrade.core.dto.response.StockReplenishmentResponseDto;
import com.globaltrade.core.dto.response.VendorFulfillmentResponseDto;
import com.globaltrade.core.dto.response.VendorResponseDto;
import com.globaltrade.core.enums.VendorStatus;
import jakarta.ejb.Local;

import java.util.List;

@Local
public interface VendorService {
    VendorResponseDto getVendorProfileByCode(String vendorCode);
    VendorResponseDto getVendorProfileByUsername(String username);
    List<VendorResponseDto> getAllVendors();
    VendorResponseDto updateVendorStatus(String vendorCode, VendorStatus newStatus, String username);
    StockReplenishmentResponseDto assignReplenishmentOrder(AssignReplenishmentVendorRequestDto request, String username);
    VendorFulfillmentResponseDto fulfillReplenishmentOrder(String replenishmentRef, String username);
    List<StockReplenishmentResponseDto> getAssignedReplenishmentsForVendor(String username);
    void evaluateAllVendorPerformances();
}