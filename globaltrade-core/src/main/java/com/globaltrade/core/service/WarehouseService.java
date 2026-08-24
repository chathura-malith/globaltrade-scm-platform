package com.globaltrade.core.service;

import com.globaltrade.core.dto.request.WarehouseRequestDto;
import com.globaltrade.core.dto.response.WarehouseResponseDto;
import jakarta.ejb.Local;

import java.util.List;

@Local
public interface WarehouseService {
    WarehouseResponseDto createWarehouse(WarehouseRequestDto request, String username);
    WarehouseResponseDto getWarehouseByCode(String warehouseCode);
    List<WarehouseResponseDto> getAllWarehouses();
}