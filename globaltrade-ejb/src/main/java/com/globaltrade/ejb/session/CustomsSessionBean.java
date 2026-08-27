package com.globaltrade.ejb.session;

import com.globaltrade.core.dto.request.CustomsClearanceActionDto;
import com.globaltrade.core.dto.request.CustomsComplianceAlertDto;
import com.globaltrade.core.dto.response.CustomsDeclarationResponseDto;
import com.globaltrade.core.dto.response.CustomsDocumentResponseDto;
import com.globaltrade.core.entity.*;
import com.globaltrade.core.enums.CustomsDocType;
import com.globaltrade.core.enums.CustomsStatus;
import com.globaltrade.core.enums.ShipmentStatus;
import com.globaltrade.core.enums.TradeAgreementType;
import com.globaltrade.core.exception.*;
import com.globaltrade.core.service.CustomsService;
import com.globaltrade.core.service.NotificationService;
import com.globaltrade.ejb.interceptor.CustomsComplianceInterceptor;
import com.globaltrade.ejb.interceptor.LogisticsAuditInterceptor;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Stateless
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Interceptors({LogisticsAuditInterceptor.class, CustomsComplianceInterceptor.class})
public class CustomsSessionBean implements CustomsService {

    private static final Set<String> SANCTIONED_COUNTRIES = Set.of("NORTH KOREA", "IRAN", "SYRIA", "CUBA");
    private static final Set<String> EU_COUNTRIES = Set.of("GERMANY", "FRANCE", "ITALY", "SPAIN", "NETHERLANDS", "BELGIUM", "POLAND", "SWEDEN");
    private static final Set<String> USMCA_COUNTRIES = Set.of("USA", "UNITED STATES", "CANADA", "MEXICO");
    private static final Set<String> ASEAN_COUNTRIES = Set.of("SINGAPORE", "MALAYSIA", "THAILAND", "VIETNAM", "INDONESIA", "PHILIPPINES");

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private NotificationService notificationService;

    @Override
    @PermitAll
    public CustomsDeclarationResponseDto generateCustomsDocumentation(Long shipmentId, String username) {
        if (shipmentId == null) {
            throw new InvalidInputException("Shipment ID is required for customs documentation generation", 400);
        }

        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
        }

        // Check if declaration already exists
        try {
            CustomsDeclaration existing = em.createQuery(
                            "SELECT cd FROM CustomsDeclaration cd WHERE cd.shipment.id = :sid", CustomsDeclaration.class)
                    .setParameter("sid", shipmentId)
                    .getSingleResult();
            return mapToResponse(existing);
        } catch (NoResultException ignored) {
            // Proceed to generate
        }

        String originCountry = shipment.getOriginAddress() != null && shipment.getOriginAddress().getCountry() != null
                ? shipment.getOriginAddress().getCountry().trim().toUpperCase() : "UNKNOWN";
        String destCountry = shipment.getDestinationAddress() != null && shipment.getDestinationAddress().getCountry() != null
                ? shipment.getDestinationAddress().getCountry().trim().toUpperCase() : "UNKNOWN";

        // 1. Sanctions Check
        boolean isSanctioned = SANCTIONED_COUNTRIES.contains(originCountry) || SANCTIONED_COUNTRIES.contains(destCountry);

        // 2. Trade Agreement & Tariff Resolution
        TradeAgreementType appliedAgreement = resolveTradeAgreement(originCountry, destCountry);
        double dutyRate = appliedAgreement.getPreferentialDutyRate();

        // 3. Valuation & Total Calculation
        BigDecimal totalDeclaredValue = BigDecimal.ZERO;
        if (shipment.getItems() != null && !shipment.getItems().isEmpty()) {
            for (ShipmentItem item : shipment.getItems()) {
                if (item.getDeclaredValue() != null) {
                    totalDeclaredValue = totalDeclaredValue.add(item.getDeclaredValue());
                } else if (item.getUnitPrice() != null && item.getQuantity() != null) {
                    totalDeclaredValue = totalDeclaredValue.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }

        BigDecimal dutyAmount = totalDeclaredValue.multiply(BigDecimal.valueOf(dutyRate)).setScale(2, RoundingMode.HALF_UP);
        String decNum = "CUST-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        CustomsStatus initialStatus = isSanctioned ? CustomsStatus.HELD_FOR_SANCTION : CustomsStatus.SUBMITTED;

        CustomsDeclaration declaration = CustomsDeclaration.builder()
                .declarationNumber(decNum)
                .shipment(shipment)
                .customsStatus(initialStatus)
                .tradeAgreementType(appliedAgreement)
                .originCountry(originCountry)
                .destinationCountry(destCountry)
                .totalDeclaredValue(totalDeclaredValue)
                .dutyRatePercentage(dutyRate * 100.0)
                .dutyAmount(dutyAmount)
                .penaltyAmount(BigDecimal.ZERO)
                .sanctionFlagged(isSanctioned)
                .submissionDeadline(LocalDateTime.now().plusHours(48))
                .inspectionNotes(isSanctioned ? "CRITICAL: Shipment flagged due to destination/origin trade sanctions embargo." : "Automated declaration generated successfully.")
                .build();

        em.persist(declaration);

        // 4. Multi-document bundle generation
        generateDocumentBundle(declaration, shipment);

        em.flush();

        // 5. Send Alert if Sanctioned or Submitted
        notificationService.sendCustomsComplianceAlert(
                CustomsComplianceAlertDto.builder()
                        .declarationNumber(decNum)
                        .trackingNumber(shipment.getTrackingNumber())
                        .originCountry(originCountry)
                        .destinationCountry(destCountry)
                        .previousStatus(CustomsStatus.DRAFT)
                        .currentStatus(initialStatus)
                        .declaredValue(totalDeclaredValue)
                        .dutyAmount(dutyAmount)
                        .tradeAgreement(appliedAgreement.name())
                        .alertSeverity(isSanctioned ? "CRITICAL_SANCTION" : "INFO")
                        .complianceIssue(isSanctioned ? "Destination embargo active. Trade sanction enforced." : "Documentation generated and submitted to customs authority.")
                        .build()
        );

        return mapToResponse(declaration);
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public CustomsDeclarationResponseDto getDeclarationByNumber(String declarationNumber) {
        return mapToResponse(findDeclarationByNumber(declarationNumber));
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public CustomsDeclarationResponseDto getDeclarationByShipmentTracking(String trackingNumber) {
        try {
            CustomsDeclaration declaration = em.createQuery(
                            "SELECT cd FROM CustomsDeclaration cd WHERE cd.shipment.trackingNumber = :track", CustomsDeclaration.class)
                    .setParameter("track", trackingNumber.trim().toUpperCase())
                    .getSingleResult();
            return mapToResponse(declaration);
        } catch (NoResultException e) {
            throw new CustomsDeclarationNotFoundException("Customs declaration record not found for shipment: " + trackingNumber, 404);
        }
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public List<CustomsDeclarationResponseDto> getAllDeclarations() {
        return em.createQuery("SELECT cd FROM CustomsDeclaration cd ORDER BY cd.createdAt DESC", CustomsDeclaration.class)
                .getResultList()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @RolesAllowed({"SYSTEM_ADMIN", "ADMIN", "CUSTOMS_OFFICER", "CUSTOMS_AGENT"})
    public CustomsDeclarationResponseDto processCustomsClearance(String declarationNumber, CustomsClearanceActionDto action, String username) {
        if (action == null || action.getStatus() == null) {
            throw new InvalidInputException("Clearance action payload and status must be provided", 400);
        }

        CustomsDeclaration declaration = findDeclarationByNumber(declarationNumber);

        if (declaration.getCustomsStatus() == CustomsStatus.CLEARED) {
            throw new InvalidCustomsStateException("Customs declaration [" + declarationNumber + "] is already CLEARED", 409);
        }

        User inspector = findUserByUsername(username);
        CustomsStatus previousStatus = declaration.getCustomsStatus();
        CustomsStatus newStatus = action.getStatus();

        declaration.setCustomsStatus(newStatus);
        declaration.setInspectedBy(inspector);
        declaration.setInspectionNotes(action.getInspectionNotes());

        if (action.getAdditionalPenaltyAmount() != null) {
            declaration.setPenaltyAmount(action.getAdditionalPenaltyAmount());
        }

        if (newStatus == CustomsStatus.CLEARED) {
            declaration.setClearanceDate(LocalDateTime.now());
            // Sync Shipment Status
            Shipment shipment = declaration.getShipment();
            if (shipment != null && shipment.getStatus() == ShipmentStatus.CREATED) {
                shipment.setStatus(ShipmentStatus.DISPATCHED);
                em.merge(shipment);
            }
        }

        declaration.setUpdatedAt(LocalDateTime.now());

        try {
            em.merge(declaration);
            em.flush();
        } catch (OptimisticLockException e) {
            throw new InvalidCustomsStateException("Concurrent modification detected during customs clearance. Please retry.", 409);
        }

        // Trigger Notification
        String severity = (newStatus == CustomsStatus.REJECTED || newStatus == CustomsStatus.HELD_FOR_SANCTION) ? "CRITICAL_SANCTION" : "INFO";
        notificationService.sendCustomsComplianceAlert(
                CustomsComplianceAlertDto.builder()
                        .declarationNumber(declaration.getDeclarationNumber())
                        .trackingNumber(declaration.getShipment() != null ? declaration.getShipment().getTrackingNumber() : "N/A")
                        .originCountry(declaration.getOriginCountry())
                        .destinationCountry(declaration.getDestinationCountry())
                        .previousStatus(previousStatus)
                        .currentStatus(newStatus)
                        .declaredValue(declaration.getTotalDeclaredValue())
                        .dutyAmount(declaration.getDutyAmount())
                        .tradeAgreement(declaration.getTradeAgreementType().name())
                        .alertSeverity(severity)
                        .complianceIssue(action.getInspectionNotes() != null ? action.getInspectionNotes() : "Customs status updated by officer.")
                        .build()
        );

        return mapToResponse(declaration);
    }

    @Override
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void evaluateCustomsDeadlinesAndCompliance() {
        LocalDateTime now = LocalDateTime.now();

        List<CustomsDeclaration> activeDeclarations = em.createQuery(
                        "SELECT cd FROM CustomsDeclaration cd WHERE cd.customsStatus IN (:statuses)", CustomsDeclaration.class)
                .setParameter("statuses", Arrays.asList(CustomsStatus.SUBMITTED, CustomsStatus.UNDER_INSPECTION))
                .getResultList();

        for (CustomsDeclaration dec : activeDeclarations) {
            if (dec.getSubmissionDeadline() != null && now.isAfter(dec.getSubmissionDeadline())) {
                CustomsStatus prev = dec.getCustomsStatus();
                dec.setCustomsStatus(CustomsStatus.DOCUMENTS_OVERDUE);
                dec.setUpdatedAt(now);
                em.merge(dec);

                notificationService.sendCustomsComplianceAlert(
                        CustomsComplianceAlertDto.builder()
                                .declarationNumber(dec.getDeclarationNumber())
                                .trackingNumber(dec.getShipment() != null ? dec.getShipment().getTrackingNumber() : "N/A")
                                .originCountry(dec.getOriginCountry())
                                .destinationCountry(dec.getDestinationCountry())
                                .previousStatus(prev)
                                .currentStatus(CustomsStatus.DOCUMENTS_OVERDUE)
                                .declaredValue(dec.getTotalDeclaredValue())
                                .dutyAmount(dec.getDutyAmount())
                                .tradeAgreement(dec.getTradeAgreementType().name())
                                .alertSeverity("ESCALATION")
                                .complianceIssue("Customs clearance deadline expired. Documentation status set to DOCUMENTS_OVERDUE.")
                                .build()
                );
            }
        }
    }

    private TradeAgreementType resolveTradeAgreement(String origin, String dest) {
        if (EU_COUNTRIES.contains(origin) && EU_COUNTRIES.contains(dest)) {
            return TradeAgreementType.EU_SINGLE_MARKET;
        }
        if (USMCA_COUNTRIES.contains(origin) && USMCA_COUNTRIES.contains(dest)) {
            return TradeAgreementType.USMCA;
        }
        if (ASEAN_COUNTRIES.contains(origin) && ASEAN_COUNTRIES.contains(dest)) {
            return TradeAgreementType.ASEAN_FTA;
        }
        return TradeAgreementType.GENERAL_MFN;
    }

    private void generateDocumentBundle(CustomsDeclaration declaration, Shipment shipment) {
        // 1. Commercial Invoice
        CustomsDocument invoice = CustomsDocument.builder()
                .customsDeclaration(declaration)
                .documentRef("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .docType(CustomsDocType.COMMERCIAL_INVOICE)
                .issuingAuthority("GlobalTrade Customs Clearance Engine")
                .documentPayload("COMMERCIAL INVOICE: Declared Value $" + declaration.getTotalDeclaredValue() + " for Shipment " + shipment.getTrackingNumber())
                .build();
        em.persist(invoice);
        declaration.getDocuments().add(invoice);

        // 2. Certificate of Origin
        CustomsDocument coo = CustomsDocument.builder()
                .customsDeclaration(declaration)
                .documentRef("COO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .docType(CustomsDocType.CERTIFICATE_OF_ORIGIN)
                .issuingAuthority("Chamber of Commerce - " + declaration.getOriginCountry())
                .documentPayload("CERTIFICATE OF ORIGIN: Verified manufacture origin: " + declaration.getOriginCountry())
                .build();
        em.persist(coo);
        declaration.getDocuments().add(coo);

        // 3. Export Declaration
        CustomsDocument exportDoc = CustomsDocument.builder()
                .customsDeclaration(declaration)
                .documentRef("EXP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .docType(CustomsDocType.EXPORT_DECLARATION)
                .issuingAuthority("Customs Border Control - " + declaration.getOriginCountry())
                .documentPayload("EXPORT PERMIT: Cargo approved for transit to " + declaration.getDestinationCountry())
                .build();
        em.persist(exportDoc);
        declaration.getDocuments().add(exportDoc);
    }

    private CustomsDeclaration findDeclarationByNumber(String decNumber) {
        if (decNumber == null || decNumber.isBlank()) {
            throw new InvalidInputException("Declaration number is required", 400);
        }
        try {
            return em.createQuery("SELECT cd FROM CustomsDeclaration cd WHERE cd.declarationNumber = :num", CustomsDeclaration.class)
                    .setParameter("num", decNumber.trim().toUpperCase())
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new CustomsDeclarationNotFoundException("Customs declaration record not found with reference: " + decNumber, 404);
        }
    }

    private User findUserByUsername(String username) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :un", User.class)
                    .setParameter("un", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new SecurityAuthenticationException("Authenticated user identity not found", 401);
        }
    }

    private CustomsDeclarationResponseDto mapToResponse(CustomsDeclaration cd) {
        List<CustomsDocumentResponseDto> docs = cd.getDocuments() != null ? cd.getDocuments().stream()
                .map(d -> CustomsDocumentResponseDto.builder()
                        .documentRef(d.getDocumentRef())
                        .docType(d.getDocType())
                        .issuingAuthority(d.getIssuingAuthority())
                        .documentPayload(d.getDocumentPayload())
                        .issuedAt(d.getIssuedAt())
                        .build())
                .collect(Collectors.toList()) : Collections.emptyList();

        return CustomsDeclarationResponseDto.builder()
                .id(cd.getId())
                .declarationNumber(cd.getDeclarationNumber())
                .trackingNumber(cd.getShipment() != null ? cd.getShipment().getTrackingNumber() : null)
                .customsStatus(cd.getCustomsStatus())
                .tradeAgreementType(cd.getTradeAgreementType())
                .originCountry(cd.getOriginCountry())
                .destinationCountry(cd.getDestinationCountry())
                .totalDeclaredValue(cd.getTotalDeclaredValue())
                .dutyRatePercentage(cd.getDutyRatePercentage())
                .dutyAmount(cd.getDutyAmount())
                .penaltyAmount(cd.getPenaltyAmount())
                .sanctionFlagged(cd.isSanctionFlagged())
                .submissionDeadline(cd.getSubmissionDeadline())
                .inspectedBy(cd.getInspectedBy() != null ? cd.getInspectedBy().getUsername() : null)
                .clearanceDate(cd.getClearanceDate())
                .inspectionNotes(cd.getInspectionNotes())
                .documents(docs)
                .createdAt(cd.getCreatedAt())
                .build();
    }
}