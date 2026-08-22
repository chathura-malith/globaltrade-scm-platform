package com.globaltrade.core.service;

import com.globaltrade.core.dto.request.ShipmentCheckpointRequestDto;
import com.globaltrade.core.dto.request.ShipmentRequestDto;
import com.globaltrade.core.dto.response.ShipmentCheckpointResponseDto;
import com.globaltrade.core.dto.response.ShipmentTrackNoResponseDto;
import com.globaltrade.core.dto.response.ShipmentTrackingResponseDto;
import com.globaltrade.core.enums.ShipmentStatus;
import jakarta.ejb.Local;

import java.util.List;

@Local
public interface ShipmentService {

    ShipmentTrackNoResponseDto createShipment(ShipmentRequestDto request, String username);

    ShipmentTrackingResponseDto getShipmentByTrackingNumber(String trackingNumber);

    List<ShipmentTrackingResponseDto> getAllShipments(int page, int size);

    ShipmentCheckpointResponseDto addCheckpoint(String trackingNumber, ShipmentCheckpointRequestDto request, String username);

    ShipmentTrackingResponseDto updateShipmentStatus(String trackingNumber, ShipmentStatus newStatus, String username);
}