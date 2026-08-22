package com.globaltrade.ejb.session;

import com.globaltrade.core.dto.request.AuditLogRequestDto;
import com.globaltrade.core.entity.AuditLog;
import com.globaltrade.core.service.AuditLogService;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;

@Stateless
public class AuditLogSessionBean implements AuditLogService {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void logAction(AuditLogRequestDto request) {
        if (request == null) {
            return;
        }

        AuditLog audit = AuditLog.builder()
                .action(request.getAction())
                .entityName(request.getEntityName())
                .entityId(request.getEntityId())
                .performedBy(request.getPerformedBy() != null ? request.getPerformedBy() : "SYSTEM")
                .details(request.getDetails())
                .timestamp(LocalDateTime.now())
                .build();

        em.persist(audit);
    }
}