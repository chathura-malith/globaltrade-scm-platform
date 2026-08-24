package com.globaltrade.ejb.session;

import com.globaltrade.core.dto.request.ItemStockRequestDto;
import com.globaltrade.core.dto.request.StockAdjustmentRequestDto;
import com.globaltrade.core.dto.response.ItemStockResponseDto;
import com.globaltrade.core.dto.response.StockReplenishmentResponseDto;
import com.globaltrade.core.entity.ItemStock;
import com.globaltrade.core.entity.StockReplenishment;
import com.globaltrade.core.exception.InsufficientStockException;
import com.globaltrade.core.exception.InvalidInputException;
import com.globaltrade.core.exception.ItemStockNotFoundException;
import com.globaltrade.core.exception.WarehouseNotFoundException;
import com.globaltrade.core.service.InventoryService;
import com.globaltrade.ejb.interceptor.LogisticsAuditInterceptor;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.stream.Collectors;

@Stateless
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Interceptors(LogisticsAuditInterceptor.class)
public class InventorySessionBean implements InventoryService {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "WAREHOUSE_MANAGER"})
    public ItemStockResponseDto createOrUpdateItemStock(ItemStockRequestDto request, String username) {
        if (request == null) {
            throw new InvalidInputException("Item stock request payload cannot be empty", 400);
        }

        validateWarehouseExists(request.getWarehouseCode());

        ItemStock stock;
        try {
            stock = em.createQuery("SELECT s FROM ItemStock s WHERE s.sku = :sku", ItemStock.class)
                    .setParameter("sku", request.getSku().trim().toUpperCase())
                    .getSingleResult();

            stock.setItemName(request.getItemName());
            stock.setWarehouseCode(request.getWarehouseCode().trim().toUpperCase());
            stock.setAvailableQuantity(stock.getAvailableQuantity() + request.getAvailableQuantity());
            stock.setReorderThreshold(request.getReorderThreshold());
            stock.setReorderQuantity(request.getReorderQuantity());
            stock.setUnitPrice(request.getUnitPrice());

            em.merge(stock);

        } catch (NoResultException e) {
            stock = ItemStock.builder()
                    .sku(request.getSku().trim().toUpperCase())
                    .itemName(request.getItemName())
                    .warehouseCode(request.getWarehouseCode().trim().toUpperCase())
                    .availableQuantity(request.getAvailableQuantity())
                    .reservedQuantity(0)
                    .reorderThreshold(request.getReorderThreshold())
                    .reorderQuantity(request.getReorderQuantity())
                    .unitPrice(request.getUnitPrice())
                    .build();

            em.persist(stock);
        }

        em.flush();
        return mapToStockResponse(stock);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public ItemStockResponseDto getItemStockBySku(String sku) {
        ItemStock stock = findStockBySku(sku);
        return mapToStockResponse(stock);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public List<ItemStockResponseDto> getStocksByWarehouse(String warehouseCode) {
        return em.createQuery("SELECT s FROM ItemStock s WHERE s.warehouseCode = :code", ItemStock.class)
                .setParameter("code", warehouseCode.trim().toUpperCase())
                .getResultList()
                .stream()
                .map(this::mapToStockResponse)
                .collect(Collectors.toList());
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "WAREHOUSE_MANAGER"})
    public ItemStockResponseDto adjustStock(StockAdjustmentRequestDto request, String username) {
        if (request == null) {
            throw new InvalidInputException("Adjustment request payload cannot be empty", 400);
        }

        ItemStock stock = findStockBySku(request.getSku());

        int updatedAvailable = stock.getAvailableQuantity() + request.getQuantityChange();
        if (updatedAvailable < 0) {
            throw new InsufficientStockException("Insufficient stock. Cannot adjust below zero for SKU: " + stock.getSku());
        }

        stock.setAvailableQuantity(updatedAvailable);

        try {
            em.merge(stock);
            em.flush();
        } catch (OptimisticLockException e) {
            throw new InsufficientStockException("Concurrent modification detected on SKU: " + stock.getSku() + ". Please retry.");
        }

        return mapToStockResponse(stock);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public ItemStockResponseDto reserveStock(String sku, int quantity, String username) {
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity to reserve must be greater than zero", 400);
        }

        ItemStock stock = findStockBySku(sku);

        if (stock.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException("Cannot reserve " + quantity + " units. Available: " + stock.getAvailableQuantity());
        }

        stock.setAvailableQuantity(stock.getAvailableQuantity() - quantity);
        stock.setReservedQuantity(stock.getReservedQuantity() + quantity);

        try {
            em.merge(stock);
            em.flush();
        } catch (OptimisticLockException e) {
            throw new InsufficientStockException("Concurrent stock update conflict on SKU: " + sku);
        }

        return mapToStockResponse(stock);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public ItemStockResponseDto releaseStock(String sku, int quantity, String username) {
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity to release must be greater than zero", 400);
        }

        ItemStock stock = findStockBySku(sku);

        if (stock.getReservedQuantity() < quantity) {
            throw new InsufficientStockException("Cannot release " + quantity + " units. Reserved: " + stock.getReservedQuantity());
        }

        stock.setReservedQuantity(stock.getReservedQuantity() - quantity);
        stock.setAvailableQuantity(stock.getAvailableQuantity() + quantity);

        try {
            em.merge(stock);
            em.flush();
        } catch (OptimisticLockException e) {
            throw new InsufficientStockException("Concurrent stock update conflict on SKU: " + sku);
        }

        return mapToStockResponse(stock);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public List<StockReplenishmentResponseDto> getAllReplenishments() {
        return em.createQuery("SELECT r FROM StockReplenishment r ORDER BY r.createdAt DESC", StockReplenishment.class)
                .getResultList()
                .stream()
                .map(this::mapToReplenishmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public ItemStockResponseDto deductReservedStock(String sku, int quantity, String username) {
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity to deduct must be greater than zero", 400);
        }

        ItemStock stock = findStockBySku(sku);

        if (stock.getReservedQuantity() < quantity) {
            throw new InsufficientStockException("Cannot deduct " + quantity + " units. Reserved: " + stock.getReservedQuantity());
        }

        stock.setReservedQuantity(stock.getReservedQuantity() - quantity);

        try {
            em.merge(stock);
            em.flush();
        } catch (OptimisticLockException e) {
            throw new InsufficientStockException("Concurrent stock update conflict on SKU: " + sku);
        }

        return mapToStockResponse(stock);
    }

    private ItemStock findStockBySku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new InvalidInputException("SKU is required", 400);
        }
        try {
            return em.createQuery("SELECT s FROM ItemStock s WHERE s.sku = :sku", ItemStock.class)
                    .setParameter("sku", sku.trim().toUpperCase())
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new ItemStockNotFoundException("Item stock not found for SKU: " + sku);
        }
    }

    private void validateWarehouseExists(String warehouseCode) {
        Long count = em.createQuery("SELECT COUNT(w) FROM Warehouse w WHERE w.warehouseCode = :code", Long.class)
                .setParameter("code", warehouseCode.trim().toUpperCase())
                .getSingleResult();

        if (count == 0) {
            throw new WarehouseNotFoundException("Warehouse not found with code: " + warehouseCode);
        }
    }

    private ItemStockResponseDto mapToStockResponse(ItemStock s) {
        return ItemStockResponseDto.builder()
                .id(s.getId())
                .sku(s.getSku())
                .itemName(s.getItemName())
                .warehouseCode(s.getWarehouseCode())
                .availableQuantity(s.getAvailableQuantity())
                .reservedQuantity(s.getReservedQuantity())
                .reorderThreshold(s.getReorderThreshold())
                .reorderQuantity(s.getReorderQuantity())
                .unitPrice(s.getUnitPrice())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private StockReplenishmentResponseDto mapToReplenishmentResponse(StockReplenishment r) {
        return StockReplenishmentResponseDto.builder()
                .id(r.getId())
                .replenishmentRef(r.getReplenishmentRef())
                .sku(r.getItemStock().getSku())
                .itemName(r.getItemStock().getItemName())
                .warehouseCode(r.getItemStock().getWarehouseCode())
                .requestedQuantity(r.getRequestedQuantity())
                .status(r.getStatus())
                .triggeredBy(r.getTriggeredBy())
                .createdAt(r.getCreatedAt())
                .build();
    }
}