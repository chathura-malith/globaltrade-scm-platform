package com.globaltrade.core.service;

import com.globaltrade.core.dto.request.CustomsClearanceActionDto;
import com.globaltrade.core.dto.response.CustomsDeclarationResponseDto;
import jakarta.ejb.Local;

import java.util.List;

@Local
public interface CustomsService {

    CustomsDeclarationResponseDto generateCustomsDocumentation(Long shipmentId, String username);

    CustomsDeclarationResponseDto getDeclarationByNumber(String declarationNumber);

    CustomsDeclarationResponseDto getDeclarationByShipmentTracking(String trackingNumber);

    List<CustomsDeclarationResponseDto> getAllDeclarations();

    CustomsDeclarationResponseDto processCustomsClearance(String declarationNumber, CustomsClearanceActionDto action, String username);

    void evaluateCustomsDeadlinesAndCompliance();
}