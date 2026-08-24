package com.globaltrade.ejb.session;

import com.globaltrade.core.dto.request.AddressRequestDto;
import com.globaltrade.core.dto.request.WarehouseRequestDto;
import com.globaltrade.core.dto.response.AddressResponseDto;
import com.globaltrade.core.dto.response.WarehouseResponseDto;
import com.globaltrade.core.entity.Address;
import com.globaltrade.core.entity.Warehouse;
import com.globaltrade.core.exception.InvalidInputException;
import com.globaltrade.core.exception.WarehouseNotFoundException;
import com.globaltrade.core.service.WarehouseService;
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
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.stream.Collectors;

@Stateless
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Interceptors(LogisticsAuditInterceptor.class)
public class WarehouseSessionBean implements WarehouseService {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN"})
    public WarehouseResponseDto createWarehouse(WarehouseRequestDto request, String username) {
        if (request == null) {
            throw new InvalidInputException("Warehouse request payload cannot be empty", 400);
        }

        String warehouseCode = generateWarehouseCode(request.getLocationAddress().getCity());

        Address location = Address.builder()
                .street(request.getLocationAddress().getStreet())
                .city(request.getLocationAddress().getCity())
                .state(request.getLocationAddress().getState())
                .postalCode(request.getLocationAddress().getPostalCode())
                .country(request.getLocationAddress().getCountry())
                .build();

        Warehouse warehouse = Warehouse.builder()
                .warehouseCode(warehouseCode)
                .warehouseName(request.getWarehouseName())
                .locationAddress(location)
                .capacityCbm(request.getCapacityCbm())
                .active(true)
                .build();

        em.persist(warehouse);
        em.flush();

        return mapToResponse(warehouse);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public WarehouseResponseDto getWarehouseByCode(String warehouseCode) {
        Warehouse warehouse = findWarehouseByCode(warehouseCode);
        return mapToResponse(warehouse);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER"})
    public List<WarehouseResponseDto> getAllWarehouses() {
        return em.createQuery("SELECT w FROM Warehouse w ORDER BY w.createdAt DESC", Warehouse.class)
                .getResultList()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Warehouse findWarehouseByCode(String warehouseCode) {
        if (warehouseCode == null || warehouseCode.isBlank()) {
            throw new InvalidInputException("Warehouse code is required", 400);
        }
        try {
            return em.createQuery(
                            "SELECT w FROM Warehouse w WHERE w.warehouseCode = :code", Warehouse.class)
                    .setParameter("code", warehouseCode.trim().toUpperCase())
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new WarehouseNotFoundException("Warehouse not found with code: " + warehouseCode);
        }
    }

    private WarehouseResponseDto mapToResponse(Warehouse w) {
        AddressResponseDto addrDto = w.getLocationAddress() != null ? AddressResponseDto.builder()
                .street(w.getLocationAddress().getStreet())
                .city(w.getLocationAddress().getCity())
                .state(w.getLocationAddress().getState())
                .postalCode(w.getLocationAddress().getPostalCode())
                .country(w.getLocationAddress().getCountry())
                .build() : null;

        return WarehouseResponseDto.builder()
                .id(w.getId())
                .warehouseCode(w.getWarehouseCode())
                .warehouseName(w.getWarehouseName())
                .locationAddress(addrDto)
                .capacityCbm(w.getCapacityCbm())
                .active(w.isActive())
                .createdAt(w.getCreatedAt())
                .build();
    }

    private String generateWarehouseCode(String city) {
        String cityPrefix = (city != null && city.trim().length() >= 3)
                ? city.trim().substring(0, 3).toUpperCase()
                : "GEN";
        String randomSuffix = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "WH-" + cityPrefix + "-" + randomSuffix;
    }
}