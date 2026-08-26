package com.globaltrade.core.service;

import com.globaltrade.core.dto.request.*;
import jakarta.ejb.Local;

@Local
public interface NotificationService {
    void sendShipmentDelayAlert(ShipmentDelayAlertDto request);
    void sendTrackingUpdateAlert(TrackingUpdateAlertDto request);
    void sendStockShortageAlert(StockShortageAlertDto request);
    void sendReplenishmentInitiatedAlert(ReplenishmentAlertDto request);
    void sendVendorPerformanceAlert(VendorPerformanceAlertDto request);
}