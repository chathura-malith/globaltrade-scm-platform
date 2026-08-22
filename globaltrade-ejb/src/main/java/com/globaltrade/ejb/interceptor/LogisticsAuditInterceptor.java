package com.globaltrade.ejb.interceptor;

import com.globaltrade.core.dto.request.AuditLogRequestDto;
import com.globaltrade.core.service.AuditLogService;
import jakarta.ejb.EJB;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogisticsAuditInterceptor {

    private static final Logger LOGGER = Logger.getLogger(LogisticsAuditInterceptor.class.getName());

    @EJB
    private AuditLogService auditLogService;

    @AroundInvoke
    public Object interceptLogisticsOperation(InvocationContext context) throws Exception {
        String targetClass = context.getTarget().getClass().getSimpleName();
        String methodName = context.getMethod().getName();
        Object[] parameters = context.getParameters();

//        String callerUsername = "ANONYMOUS";
//        if (parameters != null && parameters.length > 0) {
//            Object lastParam = parameters[parameters.length - 1];
//            if (lastParam instanceof String) {
//                callerUsername = (String) lastParam;
//            }
//        }

        String callerUsername = "ANONYMOUS";
        if (parameters != null && parameters.length > 0) {
            if ("getShipmentByTrackingNumber".equals(methodName)) {
                callerUsername = "PUBLIC_USER";
            } else {
                Object lastParam = parameters[parameters.length - 1];
                if (lastParam instanceof String) {
                    callerUsername = (String) lastParam;
                }
            }
        }

        long startTime = System.currentTimeMillis();
        LOGGER.log(Level.INFO, ">>> [INTERCEPTOR START] Invoking: {0}.{1}() by User: {2}",
                new Object[]{targetClass, methodName, callerUsername});

        Object result;
        try {
            result = context.proceed();

            long executionTime = System.currentTimeMillis() - startTime;
            String logDetails = String.format("SUCCESS | ExecutionTime: %dms | Args: %s",
                    executionTime, Arrays.toString(parameters));

            if (auditLogService != null) {
                AuditLogRequestDto auditRequest = AuditLogRequestDto.builder()
                        .action("EXECUTE_" + methodName.toUpperCase())
                        .entityName(targetClass)
                        .entityId(null)
                        .performedBy(callerUsername)
                        .details(logDetails)
                        .build();

                auditLogService.logAction(auditRequest);
            }

            LOGGER.log(Level.INFO, "<<< [INTERCEPTOR SUCCESS] {0}.{1}() completed in {2}ms",
                    new Object[]{targetClass, methodName, executionTime});

            return result;

        } catch (Exception ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorDetails = String.format("FAILED | Error: %s | ExecutionTime: %dms",
                    ex.getMessage(), executionTime);

            if (auditLogService != null) {
                AuditLogRequestDto failureRequest = AuditLogRequestDto.builder()
                        .action("FAILURE_" + methodName.toUpperCase())
                        .entityName(targetClass)
                        .entityId(null)
                        .performedBy(callerUsername)
                        .details(errorDetails)
                        .build();

                auditLogService.logAction(failureRequest);
            }

            LOGGER.log(Level.SEVERE, "<<< [INTERCEPTOR FAILURE] {0}.{1}() failed after {2}ms. Reason: {3}",
                    new Object[]{targetClass, methodName, executionTime, ex.getMessage()});

            throw ex;
        }
    }
}