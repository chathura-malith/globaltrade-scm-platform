package com.globaltrade.ejb.schedule;

import com.globaltrade.core.service.VendorService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class VendorSchedulerBean {

    private static final Logger LOGGER = Logger.getLogger(VendorSchedulerBean.class.getName());

    @EJB
    private VendorService vendorService;

    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, ">>> [AUTOMATED VENDOR PERFORMANCE SCHEDULER STARTED] Background monitoring initialized.");
    }

    // සෑම මිනිත්තු 3කට වරක් පසුබිමෙන් ස්වයංක්‍රීයව Vendor Performance & SLA Audit එක ක්‍රියාත්මක වේ
    @Schedule(minute = "*/3", hour = "*", persistent = false)
    public void runAutomatedVendorEvaluation() {
        LOGGER.log(Level.INFO, ">>> [AUTOMATED VENDOR EVALUATION TRIGGERED] Running periodic performance and SLA audit...");
        try {
            vendorService.evaluateAllVendorPerformances();
            LOGGER.log(Level.INFO, "<<< [AUTOMATED VENDOR EVALUATION COMPLETED] All vendor metrics evaluated successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "!!! [AUTOMATED VENDOR EVALUATION ERROR] Background job execution failed: {0}", e.getMessage());
        }
    }
}