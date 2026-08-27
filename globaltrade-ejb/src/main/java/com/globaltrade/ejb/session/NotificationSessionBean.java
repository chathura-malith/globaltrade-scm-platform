package com.globaltrade.ejb.session;

import com.globaltrade.core.dto.request.*;
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

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void sendVendorPerformanceAlert(VendorPerformanceAlertDto request) {
        if (request == null) {
            return;
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);

        LOGGER.log(Level.WARNING,
                "\n==================== [REAL-TIME VENDOR PERFORMANCE SLA ALERT] ====================\n" +
                        " TIMESTAMP       : {0}\n" +
                        " VENDOR CODE     : {1} ({2})\n" +
                        " NOTIFICATION TO : {3}\n" +
                        " SEVERITY        : [{4}]\n" +
                        " STATUS CHANGE   : {5} -> {6}\n" +
                        " ON-TIME RATE    : {7}%\n" +
                        " COMPLIANCE SCORE: {8}/100\n" +
                        " SLA BREACHES    : {9}\n" +
                        " ALERT DETAILS   : {10}\n" +
                        "==================================================================================",
                new Object[]{
                        timestamp,
                        request.getVendorCode(),
                        request.getVendorName(),
                        request.getRecipientEmail(),
                        request.getAlertSeverity(),
                        request.getPreviousStatus(),
                        request.getCurrentStatus(),
                        request.getOnTimeDeliveryRate(),
                        request.getCustomsComplianceScore(),
                        request.getSlaBreachCount(),
                        request.getReason()
                }
        );
    }

    @Override
    public void sendCustomsComplianceAlert(CustomsComplianceAlertDto alert) {
        String logMessage = String.format(
                "\n==================== [REAL-TIME CUSTOMS TRADE COMPLIANCE ALERT] ====================\n" +
                        " TIMESTAMP           : %s\n" +
                        " DECLARATION REF     : %s\n" +
                        " SHIPMENT TRACKING   : %s\n" +
                        " TRADE ROUTE         : %s -> %s\n" +
                        " SEVERITY            : [%s]\n" +
                        " STATUS TRANSITION   : %s -> %s\n" +
                        " DECLARED VALUE      : $%.2f\n" +
                        " CALCULATED DUTY     : $%.2f (Agreement: %s)\n" +
                        " COMPLIANCE DETAILS  : %s\n" +
                        "====================================================================================",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                alert.getDeclarationNumber(),
                alert.getTrackingNumber(),
                alert.getOriginCountry(),
                alert.getDestinationCountry(),
                alert.getAlertSeverity(),
                alert.getPreviousStatus(),
                alert.getCurrentStatus(),
                alert.getDeclaredValue() != null ? alert.getDeclaredValue().doubleValue() : 0.0,
                alert.getDutyAmount() != null ? alert.getDutyAmount().doubleValue() : 0.0,
                alert.getTradeAgreement(),
                alert.getComplianceIssue()
        );

        if ("CRITICAL_SANCTION".equalsIgnoreCase(alert.getAlertSeverity()) || "ESCALATION".equalsIgnoreCase(alert.getAlertSeverity())) {
            LOGGER.log(Level.WARNING, logMessage);
        } else {
            LOGGER.log(Level.INFO, logMessage);
        }
    }
}