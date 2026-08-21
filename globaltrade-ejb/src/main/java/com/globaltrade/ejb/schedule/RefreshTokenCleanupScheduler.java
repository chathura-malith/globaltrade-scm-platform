package com.globaltrade.ejb.schedule;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class RefreshTokenCleanupScheduler {

    private static final Logger LOGGER = Logger.getLogger(RefreshTokenCleanupScheduler.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Schedule(hour = "*", minute = "0", second = "0", persistent = false)
    public void cleanupExpiredAndRevokedTokens() {
        try {
            LOGGER.info("Starting scheduled cleanup for expired and revoked refresh tokens...");

            int deletedCount = em.createQuery(
                            "DELETE FROM RefreshToken r WHERE r.expiryDate < :now OR r.revoked = true"
                    )
                    .setParameter("now", LocalDateTime.now())
                    .executeUpdate();

            LOGGER.log(Level.INFO, "Refresh token cleanup completed. Deleted {0} expired/revoked tokens.", deletedCount);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to clean up expired refresh tokens: " + e.getMessage(), e);
        }
    }
}