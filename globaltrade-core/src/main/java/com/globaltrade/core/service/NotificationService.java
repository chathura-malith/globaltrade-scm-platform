package com.globaltrade.core.service;

import com.globaltrade.core.dto.request.ShipmentDelayAlertDto;
import com.globaltrade.core.dto.request.TrackingUpdateAlertDto;
import jakarta.ejb.Local;

@Local
public interface NotificationService {
    void sendShipmentDelayAlert(ShipmentDelayAlertDto request);
    void sendTrackingUpdateAlert(TrackingUpdateAlertDto request);
}