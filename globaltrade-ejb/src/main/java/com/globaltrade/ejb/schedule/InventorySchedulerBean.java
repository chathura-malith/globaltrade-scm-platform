package com.globaltrade.ejb.schedule;

import com.globaltrade.core.dto.request.ReplenishmentAlertDto;
import com.globaltrade.core.dto.request.StockShortageAlertDto;
import com.globaltrade.core.entity.ItemStock;
import com.globaltrade.core.entity.StockReplenishment;
import com.globaltrade.core.enums.ReplenishmentStatus;
import com.globaltrade.core.service.NotificationService;
import jakarta.ejb.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@TransactionManagement(TransactionManagementType.CONTAINER)
public class InventorySchedulerBean {

    private static final Logger LOGGER = Logger.getLogger(InventorySchedulerBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private NotificationService notificationService;

    @Schedule(hour = "*", minute = "*/2", persistent = false)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void evaluateInventoryShortagesAndReplenish() {
        LOGGER.info(">>> [INVENTORY_SCHEDULER] Starting automated inventory shortage scan & replenishment evaluation...");

        try {
            List<ItemStock> lowStockItems = em.createQuery(
                    "SELECT s FROM ItemStock s WHERE s.availableQuantity <= s.reorderThreshold",
                    ItemStock.class
            ).getResultList();

            if (lowStockItems.isEmpty()) {
                LOGGER.info(">>> [INVENTORY_SCHEDULER] All inventory stock levels are healthy. No shortages detected.");
                return;
            }

            for (ItemStock stock : lowStockItems) {
                List<StockReplenishment> activeReplenishments = em.createQuery(
                                "SELECT r FROM StockReplenishment r WHERE r.itemStock.id = :stockId " +
                                        "AND r.status IN (:activeStatuses) ORDER BY r.createdAt DESC",
                                StockReplenishment.class
                        )
                        .setParameter("stockId", stock.getId())
                        .setParameter("activeStatuses", Arrays.asList(ReplenishmentStatus.PENDING, ReplenishmentStatus.ORDERED))
                        .getResultList();

                String assignedRef;

                if (!activeReplenishments.isEmpty()) {
                    StockReplenishment existing = activeReplenishments.get(0);
                    assignedRef = existing.getReplenishmentRef() + " (" + existing.getStatus() + ")";
                    LOGGER.info(">>> [INVENTORY_SCHEDULER] Active replenishment order [" + assignedRef + "] already exists for SKU: " + stock.getSku() + ". Skipping duplicate generation.");
                } else {
                    assignedRef = "REP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

                    StockReplenishment replenishment = StockReplenishment.builder()
                            .replenishmentRef(assignedRef)
                            .itemStock(stock)
                            .requestedQuantity(stock.getReorderQuantity())
                            .status(ReplenishmentStatus.PENDING)
                            .triggeredBy("SYSTEM_SCHEDULER")
                            .createdAt(LocalDateTime.now())
                            .build();

                    em.persist(replenishment);

                    ReplenishmentAlertDto replenishmentAlertDto = ReplenishmentAlertDto.builder()
                            .replenishmentRef(assignedRef)
                            .sku(stock.getSku())
                            .itemName(stock.getItemName())
                            .warehouseCode(stock.getWarehouseCode())
                            .requestedQuantity(stock.getReorderQuantity())
                            .triggeredBy("SYSTEM_SCHEDULER")
                            .build();

                    notificationService.sendReplenishmentInitiatedAlert(replenishmentAlertDto);
                }

                StockShortageAlertDto shortageAlertDto = StockShortageAlertDto.builder()
                        .sku(stock.getSku())
                        .itemName(stock.getItemName())
                        .warehouseCode(stock.getWarehouseCode())
                        .currentStock(stock.getAvailableQuantity())
                        .threshold(stock.getReorderThreshold())
                        .replenishmentRef(assignedRef)
                        .build();

                notificationService.sendStockShortageAlert(shortageAlertDto);
            }

            em.flush();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, ">>> [INVENTORY_SCHEDULER_ERROR] Failed during inventory shortage evaluation", e);
        }
    }
}