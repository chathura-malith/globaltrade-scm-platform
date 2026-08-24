package com.globaltrade.ejb.session;

import com.globaltrade.core.dto.request.ReplenishmentAlertDto;
import com.globaltrade.core.dto.request.ShipmentDelayAlertDto;
import com.globaltrade.core.dto.request.StockShortageAlertDto;
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

        String timestamp = LocalDateTime.now().format(FORMATTER);

        LOGGER.log(Level.INFO,
                "\n==================== [REAL-TIME CARRIER TRACKING NOTIFICATION] ====================\n" +
                        " TIMESTAMP     : {0}\n" +
                        " TRACKING NO   : {1}\n" +
                        " STATUS        : {2}\n" +
                        " GPS LOCATION  : Lat: {3}, Lng: {4}\n" +
                        " REMARKS       : {5}\n" +
                        "===============================================================================",
                new Object[]{
                        timestamp,
                        request.getTrackingNumber(),
                        request.getStatus(),
                        request.getLatitude(),
                        request.getLongitude(),
                        request.getRemarks()
                }
        );
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void sendStockShortageAlert(StockShortageAlertDto request) {
        if (request == null) {
            return;
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);

        LOGGER.log(Level.WARNING,
                "\n==================== [REAL-TIME INVENTORY SHORTAGE ALERT] ====================\n" +
                        " TIMESTAMP         : {0}\n" +
                        " SKU               : {1}\n" +
                        " ITEM NAME         : {2}\n" +
                        " WAREHOUSE         : {3}\n" +
                        " CURRENT STOCK     : {4} (Threshold: {5})\n" +
                        " REPLENISHMENT REF : {6}\n" +
                        " ACTION            : Automated Replenishment Purchase Order Initiated\n" +
                        "==============================================================================",
                new Object[]{
                        timestamp, request.getSku(), request.getItemName(),
                        request.getWarehouseCode(), request.getCurrentStock(),
                        request.getThreshold(), request.getReplenishmentRef()
                }
        );
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void sendReplenishmentInitiatedAlert(ReplenishmentAlertDto request) {
        if (request == null) {
            return;
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);

        LOGGER.log(Level.INFO,
                "\n==================== [AUTOMATED REPLENISHMENT ORDER INITIATED] ====================\n" +
                        " TIMESTAMP         : {0}\n" +
                        " REPLENISHMENT REF : {1}\n" +
                        " SKU               : {2}\n" +
                        " ITEM NAME         : {3}\n" +
                        " DEST WAREHOUSE    : {4}\n" +
                        " ORDERED QUANTITY  : {5} Units\n" +
                        " TRIGGERED BY      : {6}\n" +
                        " STATUS            : PENDING (Dispatched to Supplier Portal)\n" +
                        "===================================================================================",
                new Object[]{
                        timestamp,
                        request.getReplenishmentRef(),
                        request.getSku(),
                        request.getItemName(),
                        request.getWarehouseCode(),
                        request.getRequestedQuantity(),
                        request.getTriggeredBy()
                }
        );
    }
}