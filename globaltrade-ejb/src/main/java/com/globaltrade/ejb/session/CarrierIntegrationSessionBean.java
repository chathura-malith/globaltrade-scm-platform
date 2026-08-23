package com.globaltrade.ejb.session;

import com.globaltrade.core.dto.request.AuditLogRequestDto;
import com.globaltrade.core.dto.request.BatchCarrierSyncRequestDto;
import com.globaltrade.core.dto.request.CarrierCheckpointSyncDto;
import com.globaltrade.core.dto.response.BatchCarrierSyncResponseDto;
import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.entity.ShipmentCheckpoint;
import com.globaltrade.core.entity.User;
import com.globaltrade.core.enums.ShipmentStatus;
import com.globaltrade.core.exception.InvalidInputException;
import com.globaltrade.core.exception.InvalidShipmentStateException;
import com.globaltrade.core.exception.ShipmentNotFoundException;
import com.globaltrade.core.service.AuditLogService;
import com.globaltrade.core.service.CarrierIntegrationService;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class CarrierIntegrationSessionBean implements CarrierIntegrationService {

    private static final Logger LOGGER = Logger.getLogger(CarrierIntegrationSessionBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    @EJB
    private AuditLogService auditLogService;

    @Override
    public BatchCarrierSyncResponseDto processCarrierBatchSync(BatchCarrierSyncRequestDto request, String authenticatedUser) {
        if (request == null || request.getCheckpoints() == null || request.getCheckpoints().isEmpty()) {
            throw new InvalidInputException("Batch carrier sync payload cannot be empty", 400);
        }

        UserTransaction utx = sessionContext.getUserTransaction();
        int successCount = 0;
        int failureCount = 0;
        List<String> errorMessages = new ArrayList<>();

        LOGGER.log(Level.INFO, ">>> [BMT CARRIER SYNC] Starting batch processing for carrier: {0} | Batch: {1} | Total: {2}",
                new Object[]{request.getCarrierName(), request.getBatchReference(), request.getCheckpoints().size()});

        for (CarrierCheckpointSyncDto cpDto : request.getCheckpoints()) {
            try {
                utx.begin();

                Shipment shipment;
                try {
                    shipment = em.createQuery("SELECT s FROM Shipment s WHERE s.trackingNumber = :tn", Shipment.class)
                            .setParameter("tn", cpDto.getTrackingNumber().trim().toUpperCase())
                            .getSingleResult();
                } catch (NoResultException nre) {
                    throw new ShipmentNotFoundException("Shipment tracking number not found: " + cpDto.getTrackingNumber());
                }

                if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
                    throw new InvalidShipmentStateException("Cannot add checkpoint. Shipment is already DELIVERED: " + cpDto.getTrackingNumber());
                }

                User systemActor = findSystemActor(authenticatedUser);

                ShipmentCheckpoint checkpoint = ShipmentCheckpoint.builder()
                        .shipment(shipment)
                        .locationName(cpDto.getLocationName() + " (" + request.getCarrierName() + " Hub)")
                        .latitude(cpDto.getLatitude())
                        .longitude(cpDto.getLongitude())
                        .statusAtCheckpoint(cpDto.getStatus())
                        .remarks(cpDto.getRemarks() != null ? cpDto.getRemarks() : "Carrier Batch Sync Ref: " + request.getBatchReference())
                        .updatedBy(systemActor)
                        .timestamp(cpDto.getEventTimestamp() != null ? cpDto.getEventTimestamp() : LocalDateTime.now())
                        .build();

                shipment.setStatus(cpDto.getStatus());
                if (cpDto.getLatitude() != null) shipment.setLatitude(cpDto.getLatitude());
                if (cpDto.getLongitude() != null) shipment.setLongitude(cpDto.getLongitude());
                if (cpDto.getStatus() == ShipmentStatus.DELIVERED) {
                    shipment.setActualDeliveryDate(LocalDateTime.now());
                }

                em.persist(checkpoint);
                em.merge(shipment);
                em.flush();

                utx.commit();
                successCount++;

            } catch (Exception ex) {
                try {
                    utx.rollback();
                } catch (Exception rbEx) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback individual BMT transaction", rbEx);
                }

                failureCount++;
                String error = String.format("[%s]: %s", cpDto.getTrackingNumber(), ex.getMessage());
                errorMessages.add(error);
                LOGGER.log(Level.WARNING, ">>> [BMT CARRIER SYNC ITEM FAILED] {0}", error);
            }
        }

        if (auditLogService != null) {
            auditLogService.logAction(AuditLogRequestDto.builder()
                    .action("CARRIER_BATCH_SYNC")
                    .entityName("CarrierIntegration")
                    .entityId(null)
                    .performedBy(authenticatedUser != null ? authenticatedUser : "CARRIER_API")
                    .details(String.format("Carrier: %s | BatchRef: %s | Total: %d | Success: %d | Failed: %d",
                            request.getCarrierName(), request.getBatchReference(),
                            request.getCheckpoints().size(), successCount, failureCount))
                    .build());
        }

        return BatchCarrierSyncResponseDto.builder()
                .batchReference(request.getBatchReference())
                .carrierName(request.getCarrierName())
                .totalRecords(request.getCheckpoints().size())
                .processedSuccessCount(successCount)
                .failureCount(failureCount)
                .errorDetails(errorMessages)
                .build();
    }

    private User findSystemActor(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :un", User.class)
                    .setParameter("un", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}