package com.globaltrade.core.service;

import com.globaltrade.core.dto.request.AuditLogRequestDto;
import jakarta.ejb.Local;

@Local
public interface AuditLogService {
    void logAction(AuditLogRequestDto request);
}