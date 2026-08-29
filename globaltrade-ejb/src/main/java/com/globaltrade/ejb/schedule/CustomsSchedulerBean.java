package com.globaltrade.ejb.schedule;

import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.enums.ShipmentStatus;
import com.globaltrade.core.service.CustomsService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class CustomsSchedulerBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(CustomsSchedulerBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private CustomsService customsService;

    @PostConstruct
    public void initializeScheduler() {
        LOGGER.log(Level.INFO, ">>> [CUSTOMS_SCHEDULER] Initialized Customs Automated Documentation & Deadline Monitor");
    }

    @Schedule(minute = "*/2", hour = "*", persistent = false)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void runCustomsComplianceAndDocumentationScan() {
        LOGGER.log(Level.INFO, ">>> [CUSTOMS_SCHEDULER] Starting automated customs documentation scan & deadline check...");

        try {
            List<Shipment> unmanagedShipments = em.createQuery(
                            "SELECT s FROM Shipment s WHERE s.status IN (:statuses) AND NOT EXISTS (" +
                                    "  SELECT cd FROM CustomsDeclaration cd WHERE cd.shipment.id = s.id" +
                                    ")", Shipment.class)
                    .setParameter("statuses", Arrays.asList(ShipmentStatus.CREATED, ShipmentStatus.DISPATCHED, ShipmentStatus.IN_TRANSIT))
                    .getResultList();

            if (!unmanagedShipments.isEmpty()) {
                LOGGER.log(Level.INFO, ">>> [CUSTOMS_SCHEDULER] Found {0} shipment(s) requiring automated customs documentation.",
                        unmanagedShipments.size());

                for (Shipment shipment : unmanagedShipments) {
                    try {
                        customsService.generateCustomsDocumentation(shipment.getId(), "SYSTEM_SCHEDULER");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, ">>> [CUSTOMS_SCHEDULER] Failed auto-generating documents for shipment: "
                                + shipment.getTrackingNumber() + " - Reason: " + e.getMessage());
                    }
                }
            }

            customsService.evaluateCustomsDeadlinesAndCompliance();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "!!! [CUSTOMS_SCHEDULER] Unexpected error during customs automation scan: " + e.getMessage(), e);
        }
    }
}