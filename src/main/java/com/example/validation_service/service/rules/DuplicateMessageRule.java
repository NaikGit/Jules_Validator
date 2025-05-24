package com.example.validation_service.service.rules;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.DisposableBean;

@Component
public class DuplicateMessageRule implements ValidationRule, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateMessageRule.class);

    private final int cacheSize;
    private final long ttlSeconds;
    private final LinkedHashMap<String, Long> messageCache; // MsgId -> Insertion Timestamp (ms)

    // Using a ScheduledExecutorService to periodically clean up expired entries
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();

    public DuplicateMessageRule(
            @Value("${validation.rules.duplicate.cache.size:1000}") int cacheSize,
            @Value("${validation.rules.duplicate.cache.ttl-seconds:3600}") long ttlSeconds) {
        this.cacheSize = cacheSize;
        this.ttlSeconds = ttlSeconds;
        // Initialize LinkedHashMap for LRU behavior (accessOrder = true)
        this.messageCache = new LinkedHashMap<String, Long>(cacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                // Remove eldest if cache size is exceeded
                return size() > DuplicateMessageRule.this.cacheSize;
            }
        };
        logger.info("Initialized DuplicateMessageRule with cache size: {}, TTL: {} seconds", cacheSize, ttlSeconds);
        // Schedule cleanup task if TTL is positive
        if (this.ttlSeconds > 0) {
            cleanupScheduler.scheduleAtFixedRate(this::removeExpiredEntries, ttlSeconds, ttlSeconds / 2, TimeUnit.SECONDS);
            logger.info("Scheduled cache cleanup task to run every {} seconds.", ttlSeconds / 2);
        }
    }

    @Override
    public void validate(RawPaymentData data, ValidationResult result) {
        if (data.getMsgId() == null || data.getMsgId().trim().isEmpty()) {
            // This should ideally be caught by schema validation, but good to be defensive.
            result.addError("MsgId is missing, cannot perform duplicate check.");
            return;
        }

        String msgId = data.getMsgId();
        long currentTimeMillis = System.currentTimeMillis();

        synchronized (messageCache) {
            if (messageCache.containsKey(msgId)) {
                Long entryTimestamp = messageCache.get(msgId);
                if (ttlSeconds <= 0 || (currentTimeMillis - entryTimestamp) < (ttlSeconds * 1000)) {
                    result.addError("Duplicate message detected: MsgId " + msgId + " is already processed.");
                    logger.warn("Duplicate MsgId {} detected.", msgId);
                } else {
                    // Entry expired, remove it and allow reprocessing (or treat as new)
                    messageCache.remove(msgId);
                    messageCache.put(msgId, currentTimeMillis);
                    logger.info("MsgId {} was found in cache but expired. Treating as new.", msgId);
                }
            } else {
                messageCache.put(msgId, currentTimeMillis);
                logger.debug("MsgId {} added to duplicate check cache.", msgId);
            }
        }
    }

    private void removeExpiredEntries() {
        if (ttlSeconds <= 0) return; // No TTL, no cleanup needed

        long currentTimeMillis = System.currentTimeMillis();
        long expiryThreshold = currentTimeMillis - (ttlSeconds * 1000);

        synchronized (messageCache) {
            // Iterate and remove expired entries.
            // Need to use an iterator to safely remove while iterating or collect keys to remove.
            messageCache.entrySet().removeIf(entry -> entry.getValue() < expiryThreshold);
            logger.info("Cache cleanup task executed. Current cache size: {}", messageCache.size());
        }
    }

    // Ensure scheduler is shut down when the bean is destroyed
    @Override
    public void destroy() throws Exception {
        logger.info("Shutting down DuplicateMessageRule cleanup scheduler (destroy method).");
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Cleanup scheduler interrupted during shutdown.", e);
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("DuplicateMessageRule cleanup scheduler shutdown complete.");
    }
}
