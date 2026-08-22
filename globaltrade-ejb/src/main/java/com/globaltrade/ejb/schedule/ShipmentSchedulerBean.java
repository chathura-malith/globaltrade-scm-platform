package com.globaltrade.ejb.schedule;

import com.globaltrade.core.dto.request.AuditLogRequestDto;
import com.globaltrade.core.dto.request.ShipmentDelayAlertDto;
import com.globaltrade.core.dto.request.TrackingUpdateAlertDto;
import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.enums.ShipmentStatus;
import com.globaltrade.core.service.AuditLogService;
import com.globaltrade.core.service.NotificationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ShipmentSchedulerBean {

    private static final Logger LOGGER = Logger.getLogger(ShipmentSchedulerBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    @EJB
    private NotificationService notificationService;

    @EJB
    private AuditLogService auditLogService;

    // --- REQUIREMENT 1: Automated Scheduling Service for Shipment Tracking (Programmatic Timer) ---
    @PostConstruct
    public void initializeAutomatedShipmentTrackingTimer() {
        TimerConfig timerConfig = new TimerConfig("AUTOMATED_SCHEDULED_SHIPMENT_TRACKING_TIMER", false);
        TimerService timerService = sessionContext.getTimerService();
        timerService.createIntervalTimer(10000L, 45000L, timerConfig);
        LOGGER.log(Level.INFO, ">>> [TIMER SERVICE: SHIPMENT TRACKING] Programmatic Timer Initialized successfully (Interval: 45s).");
    }

    @Timeout
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void executeAutomatedShipmentTrackingSync(Timer timer) {
        if (!"AUTOMATED_SCHEDULED_SHIPMENT_TRACKING_TIMER".equals(timer.getInfo())) {
            return;
        }

        try {
            List<Shipment> transitShipments = em.createQuery(
                            "SELECT s FROM Shipment s WHERE s.status IN (:s1, :s2)", Shipment.class)
                    .setParameter("s1", ShipmentStatus.DISPATCHED)
                    .setParameter("s2", ShipmentStatus.IN_TRANSIT)
                    .setMaxResults(5)
                    .getResultList();

            for (Shipment s : transitShipments) {
                double currentLat = s.getLatitude() != null ? s.getLatitude() + 0.0050 : 6.9271;
                double currentLng = s.getLongitude() != null ? s.getLongitude() + 0.0050 : 79.8612;

                s.setLatitude(currentLat);
                s.setLongitude(currentLng);
                em.merge(s);

                notificationService.sendTrackingUpdateAlert(TrackingUpdateAlertDto.builder()
                        .trackingNumber(s.getTrackingNumber())
                        .status(s.getStatus().name())
                        .latitude(currentLat)
                        .longitude(currentLng)
                        .remarks("Automated Carrier GPS synchronization update")
                        .build());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing Automated Shipment Tracking Sync: {0}", e.getMessage());
        }
    }

    // --- REQUIREMENT 2: Real-time Supply Chain Monitoring with Alerts for Shipment Delays (Declarative Timer) ---
    @Schedule(minute = "*/1", hour = "*", persistent = false)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void executeRealTimeShipmentDelayMonitoring() {
        LOGGER.log(Level.INFO, ">>> [TIMER SERVICE: SHIPMENT DELAYS] Real-time Shipment Delay Scan Started...");

        LocalDateTime now = LocalDateTime.now();

        List<Shipment> delayedShipments = em.createQuery(
                        "SELECT s FROM Shipment s WHERE s.status NOT IN (:delivered, :cancelled) " +
                                "AND s.estimatedDeliveryDate < :now", Shipment.class)
                .setParameter("delivered", ShipmentStatus.DELIVERED)
                .setParameter("cancelled", ShipmentStatus.CANCELLED)
                .setParameter("now", now)
                .getResultList();

        if (delayedShipments.isEmpty()) {
            LOGGER.log(Level.INFO, ">>> [TIMER SERVICE: SHIPMENT DELAYS] No delayed shipments detected during scan.");
            return;
        }

        for (Shipment s : delayedShipments) {
            long overdueHours = ChronoUnit.HOURS.between(s.getEstimatedDeliveryDate(), now);
            String recipientEmail = (s.getCreatedBy() != null && s.getCreatedBy().getEmail() != null)
                    ? s.getCreatedBy().getEmail()
                    : "logistics-operations@globaltrade.com";

            String delayReason = String.format("Shipment %s is overdue by %d hour(s). Expected: %s, Current Time: %s",
                    s.getTrackingNumber(), overdueHours, s.getEstimatedDeliveryDate(), now);

            // 1. Dispatch Real-time Alert Notification
            notificationService.sendShipmentDelayAlert(ShipmentDelayAlertDto.builder()
                    .trackingNumber(s.getTrackingNumber())
                    .recipientEmail(recipientEmail)
                    .delayReason(delayReason)
                    .overdueHours(overdueHours)
                    .build());

            // 2. Log Autonomous Audit Trail Record (REQUIRES_NEW)
            auditLogService.logAction(AuditLogRequestDto.builder()
                    .action("ALERT_SHIPMENT_DELAY_DETECTED")
                    .entityName("Shipment")
                    .entityId(s.getId())
                    .performedBy("EJB_DELAY_MONITOR_TIMER")
                    .details(String.format("REALTIME_DELAY_ALERT | Overdue: %dh | TrackingNo: %s | Status: %s",
                            overdueHours, s.getTrackingNumber(), s.getStatus()))
                    .build());
        }
    }
}