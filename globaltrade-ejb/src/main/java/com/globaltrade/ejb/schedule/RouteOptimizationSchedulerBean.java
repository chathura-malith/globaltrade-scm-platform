package com.globaltrade.ejb.schedule;

import com.globaltrade.core.service.RouteOptimizationService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.*;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class RouteOptimizationSchedulerBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(RouteOptimizationSchedulerBean.class.getName());

    @EJB
    private RouteOptimizationService routeOptimizationService;

    @PostConstruct
    public void initializeScheduler() {
        LOGGER.log(Level.INFO, ">>> [ROUTE_SCHEDULER] Initialized Route Optimization Engine & Transit SLA Monitor");
    }

    @Schedule(minute = "*/3", hour = "*", persistent = false)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void runAutomatedRouteOptimizationScan() {
        LOGGER.log(Level.INFO, ">>> [ROUTE_SCHEDULER] Running automated periodic route evaluation and re-routing scan...");
        try {
            routeOptimizationService.evaluateAndReOptimizeActiveShipments();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "!!! [ROUTE_SCHEDULER] Exception during automated route optimization execution: " + e.getMessage(), e);
        }
    }
}