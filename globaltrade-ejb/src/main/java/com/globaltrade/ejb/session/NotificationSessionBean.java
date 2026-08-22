package com.globaltrade.ejb.session;

import com.globaltrade.core.dto.request.ShipmentDelayAlertDto;
import com.globaltrade.core.dto.request.TrackingUpdateAlertDto;
import com.globaltrade.core.service.NotificationService;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class NotificationSessionBean implements NotificationService {

    private static final Logger LOGGER = Logger.getLogger(NotificationSessionBean.class.getName());
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void sendShipmentDelayAlert(ShipmentDelayAlertDto request) {
        if (request == null) {
            return;
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);

        LOGGER.log(Level.WARNING,
                "\n==================== [REAL-TIME SHIPMENT DELAY ALERT] ====================\n" +
                        " TIMESTAMP     : {0}\n" +
                        " TO            : {1}\n" +
                        " SUBJECT       : URGENT: Shipment {2} Schedule Exception\n" +
                        " OVERDUE HOURS : {3}h\n" +
                        " DETAILS       : {4}\n" +
                        "==========================================================================",
                new Object[]{timestamp, request.getRecipientEmail(), request.getTrackingNumber(), request.getOverdueHours(), request.getDelayReason()}
        );
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void sendTrackingUpdateAlert(TrackingUpdateAlertDto request) {
        if (request == null) {
            return;
        }

        LOGGER.log(Level.INFO,
                ">>> [CARRIER SYNC NOTIFICATION] TrackingNo: {0} | Status: {1} | Location: Lat: {2}, Lng: {3} | Remarks: {4}",
                new Object[]{request.getTrackingNumber(), request.getStatus(), request.getLatitude(), request.getLongitude(), request.getRemarks()}
        );
    }
}