package com.globaltrade.core.service;

import com.globaltrade.core.dto.request.RouteOptimizationRequestDto;
import com.globaltrade.core.dto.response.RouteOptimizationResponseDto;
import jakarta.ejb.Local;

import java.util.List;

@Local
public interface RouteOptimizationService {

    RouteOptimizationResponseDto calculateAndCreateOptimizedRoute(Long shipmentId, RouteOptimizationRequestDto request, String username);

    RouteOptimizationResponseDto getPlanByReference(String planReference);

    List<RouteOptimizationResponseDto> getPlansByShipmentTracking(String trackingNumber);

    List<RouteOptimizationResponseDto> getAllRoutePlans();

    RouteOptimizationResponseDto activateRoutePlan(String planReference, String username);

    void evaluateAndReOptimizeActiveShipments();
}