package com.globaltrade.ejb.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomsComplianceInterceptor implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(CustomsComplianceInterceptor.class.getName());
    private static final long COMPLIANCE_SLA_THRESHOLD_MS = 200;

    @AroundInvoke
    public Object auditAndEnforceTradeCompliance(InvocationContext context) throws Exception {
        String methodName = context.getMethod().getName();
        String targetClass = context.getTarget().getClass().getSimpleName();

        LOGGER.log(Level.INFO, ">>> [CUSTOMS COMPLIANCE INTERCEPTOR START] Validating trade protocol for: {0}.{1}()",
                new Object[]{targetClass, methodName});

        long startTime = System.currentTimeMillis();
        Object result;

        try {
            result = context.proceed();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "!!! [CUSTOMS COMPLIANCE REJECTION] Exception intercepted in {0}.{1}(): {2}",
                    new Object[]{targetClass, methodName, e.getMessage()});
            throw e;
        }

        long duration = System.currentTimeMillis() - startTime;

        if (duration > COMPLIANCE_SLA_THRESHOLD_MS) {
            LOGGER.log(Level.WARNING,
                    "\n======================================================================\n" +
                            " >>> [CUSTOMS LATENCY SLA WARNING] High Processing Latency!\n" +
                            " Target Method : {0}.{1}()\n" +
                            " Latency       : {2} ms\n" +
                            " SLA Threshold : {3} ms\n" +
                            " Status        : POTENTIAL TRADE CLEARANCE BOTTLENECK\n" +
                            "======================================================================",
                    new Object[]{targetClass, methodName, duration, COMPLIANCE_SLA_THRESHOLD_MS});
        } else {
            LOGGER.log(Level.INFO,
                    "\n----------------------------------------------------------------------\n" +
                            " <<< [CUSTOMS INTERCEPTOR AUDIT SUCCESS] Execution verified\n" +
                            " Target Method : {0}.{1}()\n" +
                            " Duration      : {2} ms (Compliant SLA)\n" +
                            "----------------------------------------------------------------------",
                    new Object[]{targetClass, methodName, duration});
        }

        return result;
    }
}