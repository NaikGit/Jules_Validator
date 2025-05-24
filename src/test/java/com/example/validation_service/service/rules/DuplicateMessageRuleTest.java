package com.example.validation_service.service.rules;

import com.example.validation_service.dto.RawPaymentData;
import com.example.validation_service.service.ValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;


public class DuplicateMessageRuleTest {

    private DuplicateMessageRule duplicateMessageRule;
    private RawPaymentData rawPaymentData1;
    private RawPaymentData rawPaymentData2_duplicate;
    private RawPaymentData rawPaymentData3_different;
    private ValidationResult validationResult;

    private final int CACHE_SIZE = 10;
    private final long TTL_SECONDS = 2; // Short TTL for testing expiry

    private MockedStatic<System> mockedSystem;

    @BeforeEach
    void setUp() {
        // Mock System.currentTimeMillis() to control time for TTL testing
        mockedSystem = Mockito.mockStatic(System.class, Mockito.CALLS_REAL_METHODS);

        duplicateMessageRule = new DuplicateMessageRule(CACHE_SIZE, TTL_SECONDS);
        
        rawPaymentData1 = new RawPaymentData();
        rawPaymentData1.setMsgId("MSGID001");

        rawPaymentData2_duplicate = new RawPaymentData();
        rawPaymentData2_duplicate.setMsgId("MSGID001"); // Same MsgId as rawPaymentData1

        rawPaymentData3_different = new RawPaymentData();
        rawPaymentData3_different.setMsgId("MSGID002");


        validationResult = new ValidationResult();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (duplicateMessageRule != null) {
            duplicateMessageRule.destroy(); // Call destroy to shutdown scheduler
        }
        if (mockedSystem != null) {
            mockedSystem.close();
        }
    }

    @Test
    void testValidate_NewMessage_SuccessAndCached() {
        mockedSystem.when(System::currentTimeMillis).thenReturn(1000L);
        duplicateMessageRule.validate(rawPaymentData1, validationResult);
        assertFalse(validationResult.hasErrors());
        
        // Try to validate again immediately (within TTL) - should fail
        validationResult = new ValidationResult(); // Reset result
        duplicateMessageRule.validate(rawPaymentData2_duplicate, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals("Duplicate message detected: MsgId MSGID001 is already processed.", validationResult.getErrors().get(0));
    }

    @Test
    void testValidate_DuplicateMessageWithinTtl_Failure() {
        mockedSystem.when(System::currentTimeMillis).thenReturn(1000L);
        duplicateMessageRule.validate(rawPaymentData1, validationResult);
        assertFalse(validationResult.hasErrors());

        // Simulate time passing but still within TTL
        mockedSystem.when(System::currentTimeMillis).thenReturn(1000L + (TTL_SECONDS * 1000) - 500); // 0.5 sec before TTL expires
        
        validationResult = new ValidationResult(); // Reset result
        duplicateMessageRule.validate(rawPaymentData2_duplicate, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals("Duplicate message detected: MsgId MSGID001 is already processed.", validationResult.getErrors().get(0));
    }

    @Test
    void testValidate_DuplicateMessageAfterTtl_SuccessAndRecached() {
        long initialTime = 1000L;
        long timeAfterTtl = initialTime + (TTL_SECONDS * 1000) + 500; // 0.5 sec after TTL expires

        mockedSystem.when(System::currentTimeMillis).thenReturn(initialTime);
        duplicateMessageRule.validate(rawPaymentData1, validationResult);
        assertFalse(validationResult.hasErrors(), "First validation should pass");

        // Simulate time passing beyond TTL for the cache entry and for cleanup task to run
        mockedSystem.when(System::currentTimeMillis).thenReturn(timeAfterTtl);

        // Wait for the cleanup scheduler to potentially run and remove the expired entry.
        // The cleanup task runs at TTL_SECONDS / 2 interval, but first run is after TTL_SECONDS.
        // We need to wait enough for the entry to be considered expired by the cleanup.
        // The cache cleanup is not immediate, it depends on the scheduler.
        // Awaitility can help here if the cleanup was more directly testable,
        // but for internal LinkedHashMap TTL, direct time control is key.
        // The rule's internal cleanup method `removeExpiredEntries` is called by a scheduler.
        // For this test, we rely on the fact that if `containsKey` finds an expired entry, it removes it.
        // Or, if the cache is full, LinkedHashMap itself would remove the eldest.

        // To more reliably test TTL expiry by the rule's own cleanup thread,
        // we'd need a more complex setup or make the cleanup triggerable.
        // For now, let's test the behavior when validate is called and an entry *would have* expired.
        // The `validate` method itself checks TTL if `ttlSeconds > 0`.
        
        // If the message is re-validated after TTL, it should be treated as new.
        validationResult = new ValidationResult(); // Reset result
        duplicateMessageRule.validate(rawPaymentData2_duplicate, validationResult);
        assertFalse(validationResult.hasErrors(), "Validation of same MsgId after TTL should pass and re-cache.");

        // And now, if we try again immediately, it should be a duplicate again
        validationResult = new ValidationResult();
        mockedSystem.when(System::currentTimeMillis).thenReturn(timeAfterTtl + 10); // Small increment, still "same" time
        duplicateMessageRule.validate(rawPaymentData2_duplicate, validationResult);
        assertTrue(validationResult.hasErrors(), "Should be a duplicate again after re-caching.");
    }
    
    @Test
    void testValidate_CacheEviction_OldestRemovedWhenFull() {
        // Set a small cache size for this specific test instance
        DuplicateMessageRule smallCacheRule = new DuplicateMessageRule(2, TTL_SECONDS);
        try {
            mockedSystem.when(System::currentTimeMillis).thenReturn(1000L);

            RawPaymentData data1 = new RawPaymentData(); data1.setMsgId("MSG001");
            RawPaymentData data2 = new RawPaymentData(); data2.setMsgId("MSG002");
            RawPaymentData data3 = new RawPaymentData(); data3.setMsgId("MSG003");

            smallCacheRule.validate(data1, new ValidationResult()); // Cache: MSG001
            mockedSystem.when(System::currentTimeMillis).thenReturn(1010L);
            smallCacheRule.validate(data2, new ValidationResult()); // Cache: MSG001, MSG002

            // Access MSG001 to make it most recently used (if accessOrder=true, which it is)
            mockedSystem.when(System::currentTimeMillis).thenReturn(1020L);
            smallCacheRule.validate(data1, new ValidationResult()); // Should be a duplicate error
            
            // Now add MSG003, this should evict MSG002 because MSG001 was more recently accessed
            mockedSystem.when(System::currentTimeMillis).thenReturn(1030L);
            smallCacheRule.validate(data3, new ValidationResult()); // Cache: MSG001, MSG003 (MSG002 evicted)


            // Validate MSG002 again, it should pass as it was evicted
            ValidationResult resultForData2 = new ValidationResult();
            mockedSystem.when(System::currentTimeMillis).thenReturn(1040L);
            smallCacheRule.validate(data2, resultForData2);
            assertFalse(resultForData2.hasErrors(), "MSG002 should be accepted as it was evicted");

            // Validate MSG001, should still be a duplicate
            ValidationResult resultForData1 = new ValidationResult();
            mockedSystem.when(System::currentTimeMillis).thenReturn(1050L);
            smallCacheRule.validate(data1, resultForData1); // This would be data1.setMsgId("MSG001");
            assertTrue(resultForData1.hasErrors(), "MSG001 should still be a duplicate");

        } finally {
            smallCacheRule.destroy();
        }
    }


    @Test
    void testValidate_NullMsgId_AddsError() {
        RawPaymentData dataWithNullMsgId = new RawPaymentData();
        dataWithNullMsgId.setMsgId(null);
        duplicateMessageRule.validate(dataWithNullMsgId, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals("MsgId is missing, cannot perform duplicate check.", validationResult.getErrors().get(0));
    }

    @Test
    void testValidate_EmptyMsgId_AddsError() {
        RawPaymentData dataWithEmptyMsgId = new RawPaymentData();
        dataWithEmptyMsgId.setMsgId("");
        duplicateMessageRule.validate(dataWithEmptyMsgId, validationResult);
        assertTrue(validationResult.hasErrors());
        assertEquals("MsgId is missing, cannot perform duplicate check.", validationResult.getErrors().get(0));
    }

    @Test
    void testDestroy_ShutsDownScheduler() throws Exception {
        // This test is a bit indirect. We're checking if destroy() can be called without error.
        // And that after destroy, if we were to advance time significantly for scheduled tasks, they wouldn't run.
        // However, directly testing scheduler state (isShutdown, isTerminated) is more robust if possible.
        // DuplicateMessageRule's scheduler is private.
        // For now, just calling destroy and ensuring no exceptions.
        assertDoesNotThrow(() -> duplicateMessageRule.destroy());
        // To be more thorough, one might use Awaitility to check for scheduler termination
        // if the scheduler was exposed or if its state could be indirectly observed.
    }
}
