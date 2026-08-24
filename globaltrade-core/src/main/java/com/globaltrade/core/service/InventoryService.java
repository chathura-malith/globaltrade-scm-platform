package com.globaltrade.core.service;

import com.globaltrade.core.dto.request.ItemStockRequestDto;
import com.globaltrade.core.dto.request.StockAdjustmentRequestDto;
import com.globaltrade.core.dto.response.ItemStockResponseDto;
import com.globaltrade.core.dto.response.StockReplenishmentResponseDto;
import jakarta.ejb.Local;

import java.util.List;

@Local
public interface InventoryService {
    ItemStockResponseDto createOrUpdateItemStock(ItemStockRequestDto request, String username);
    ItemStockResponseDto getItemStockBySku(String sku);
    List<ItemStockResponseDto> getStocksByWarehouse(String warehouseCode);
    ItemStockResponseDto adjustStock(StockAdjustmentRequestDto request, String username);
    ItemStockResponseDto reserveStock(String sku, int quantity, String username);
    ItemStockResponseDto releaseStock(String sku, int quantity, String username);
    List<StockReplenishmentResponseDto> getAllReplenishments();
    ItemStockResponseDto deductReservedStock(String sku, int quantity, String username);
}