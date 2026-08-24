package com.globaltrade.core.service;

import com.globaltrade.core.dto.request.ReplenishmentAlertDto;
import com.globaltrade.core.dto.request.ShipmentDelayAlertDto;
import com.globaltrade.core.dto.request.StockShortageAlertDto;
import com.globaltrade.core.dto.request.TrackingUpdateAlertDto;
import jakarta.ejb.Local;

@Local
public interface NotificationService {
    void sendShipmentDelayAlert(ShipmentDelayAlertDto request);
    void sendTrackingUpdateAlert(TrackingUpdateAlertDto request);
    void sendStockShortageAlert(StockShortageAlertDto request);
    void sendReplenishmentInitiatedAlert(ReplenishmentAlertDto request);
}