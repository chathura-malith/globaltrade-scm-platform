package com.globaltrade.ejb.interceptor;

import com.globaltrade.core.dto.request.AssignReplenishmentVendorRequestDto;
import com.globaltrade.core.entity.VendorPerformance;
import com.globaltrade.core.enums.VendorStatus;
import com.globaltrade.core.exception.VendorNotFoundException;
import com.globaltrade.core.exception.VendorSuspendedException;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VendorValidationInterceptor implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(VendorValidationInterceptor.class.getName());
    private static final long PERFORMANCE_THRESHOLD_MS = 200;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @AroundInvoke
    public Object validateAndMonitorVendorOperation(InvocationContext context) throws Exception {
        String methodName = context.getMethod().getName();
        String className = context.getTarget().getClass().getSimpleName();
        Object[] parameters = context.getParameters();

        LOGGER.log(Level.INFO, ">>> [VENDOR INTERCEPTOR START] Invoking: {0}.{1}()", new Object[]{className, methodName});

        boolean isExemptMethod = methodName.startsWith("get")
                || methodName.equals("updateVendorStatus")
                || methodName.equals("getAllVendors");

        if (!isExemptMethod && parameters != null) {
            for (Object param : parameters) {
                if (param instanceof AssignReplenishmentVendorRequestDto dto) {
                    validateVendorStatusByIdentifier(dto.getVendorIdentifier());
                } else if (param instanceof String identifier && methodName.toLowerCase().contains("vendor")) {
                    if (identifier.startsWith("VND-")) {
                        validateVendorStatusByIdentifier(identifier);
                    }
                }
            }
        }

        long startTime = System.currentTimeMillis();
        Object result;

        try {
            result = context.proceed();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "!!! [VENDOR INTERCEPTOR EXCEPTION] Exception in {0}.{1}(): {2}",
                    new Object[]{className, methodName, e.getMessage()});
            throw e;
        }

        long executionDuration = System.currentTimeMillis() - startTime;

        if (executionDuration > PERFORMANCE_THRESHOLD_MS) {
            LOGGER.log(Level.WARNING,
                    "\n======================================================================\n" +
                            " >>> [VENDOR PERFORMANCE SLA ALERT] High Latency Detected!\n" +
                            " Target Method : {0}.{1}()\n" +
                            " Duration      : {2} ms\n" +
                            " SLA Threshold : {3} ms\n" +
                            " Status        : POTENTIAL SUPPLY CHAIN PERFORMANCE BREACH\n" +
                            "======================================================================",
                    new Object[]{className, methodName, executionDuration, PERFORMANCE_THRESHOLD_MS});
        } else {
            LOGGER.log(Level.INFO,
                    "\n----------------------------------------------------------------------\n" +
                            " <<< [VENDOR INTERCEPTOR SUCCESS] Invocation Completed\n" +
                            " Target Method : {0}.{1}()\n" +
                            " Latency       : {2} ms (Optimal SLA)\n" +
                            "----------------------------------------------------------------------",
                    new Object[]{className, methodName, executionDuration});
        }

        return result;
    }

    private void validateVendorStatusByIdentifier(String vendorIdentifier) {
        if (vendorIdentifier == null || vendorIdentifier.isBlank()) {
            return;
        }

        try {
            VendorPerformance vendor = em.createQuery(
                            "SELECT vp FROM VendorPerformance vp WHERE vp.vendorCode = :ident OR vp.vendorUser.username = :ident",
                            VendorPerformance.class)
                    .setParameter("ident", vendorIdentifier.trim())
                    .getSingleResult();

            if (vendor.getStatus() == VendorStatus.SUSPENDED) {
                throw new VendorSuspendedException("Vendor account [" + vendor.getVendorCode() + "] is currently SUSPENDED due to SLA breaches or compliance violations.", 403);
            }

            if (vendor.getStatus() == VendorStatus.UNDER_REVIEW) {
                throw new VendorSuspendedException("Vendor account [" + vendor.getVendorCode() + "] is currently UNDER_REVIEW. Orders cannot be assigned.", 403);
            }

            if (vendor.getStatus() == VendorStatus.INACTIVE) {
                throw new VendorSuspendedException("Vendor account [" + vendor.getVendorCode() + "] is INACTIVE in the GlobalTrade platform.", 403);
            }

        } catch (NoResultException e) {
            throw new VendorNotFoundException("Vendor record not found for identifier: " + vendorIdentifier, 404);
        }
    }
}