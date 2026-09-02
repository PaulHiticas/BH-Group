package com.bhgroup.pms.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bhgroup.pms.config.AppProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Covers exactly the transaction-timing decision in EmailService.send():
 * dispatch immediately when there's no active transaction, defer until
 * afterCommit when there is one. Uses the real TransactionSynchronizationManager
 * (a thread-local test double is unnecessary - Spring's own utility is
 * designed to be driven directly like this in tests).
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailDispatcher emailDispatcher;

    private AppProperties appProperties;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setName("BH Group PMS");
        appProperties.setBaseUrl("https://app.bhgroup.io");
        appProperties.getMail().setFrom("no-reply@bhgroup.io");

        emailService = new EmailService(emailDispatcher, appProperties);
    }

    @AfterEach
    void clearAnyLeftoverSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void send_dispatchesImmediately_whenNoTransactionIsActive() {
        emailService.sendPasswordResetEmail("guest@example.com", "Ana", "raw-token", 60);

        verify(emailDispatcher).dispatch(
                eq("guest@example.com"), anyString(), eq("email/password-reset-email"), any());
    }

    @Test
    void send_defersDispatchUntilAfterCommit_whenATransactionIsActive() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            emailService.sendPasswordResetEmail("guest@example.com", "Ana", "raw-token", 60);

            // Not sent yet - the (simulated) transaction hasn't committed.
            verify(emailDispatcher, never()).dispatch(any(), any(), any(), any());

            // Simulate the commit: this is exactly what Spring's real
            // transaction infrastructure does when a @Transactional method
            // returns successfully.
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(emailDispatcher).dispatch(
                eq("guest@example.com"), anyString(), eq("email/password-reset-email"), any());
    }

    @Test
    void send_neverDispatches_whenATransactionIsActiveButNeverCommits() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            emailService.sendPasswordResetEmail("guest@example.com", "Ana", "raw-token", 60);
            // Simulate a rollback: no afterCommit() call at all.
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(emailDispatcher, never()).dispatch(any(), any(), any(), any());
    }
}
