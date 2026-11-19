package com.ericksoares.tattoo.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real transactions against Postgres, so deliberately not @Transactional. The shared test
 * database may hold any number of other admins, so instead of asserting "one demotion fails"
 * (which depends on the global admin count) this proves the mechanism: while one transaction
 * holds the guard's lock, a second guard call blocks until the first commits.
 */
@SpringBootTest
class LastAdminGuardConcurrencyIT {

    @Autowired
    private LastAdminGuard guard;

    @Autowired
    private UserRepository repository;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long adminA;
    private Long adminB;

    @BeforeEach
    void createAdmins() {
        long suffix = System.nanoTime();
        adminA = UserTestFixtures.createUser(context, "lock-a-" + suffix + "@test.com", UserType.ADMIN).getId();
        adminB = UserTestFixtures.createUser(context, "lock-b-" + suffix + "@test.com", UserType.ADMIN).getId();
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAllById(List.of(adminA, adminB));
    }

    @Test
    void shouldMakeASecondGuardCallWaitUntilTheFirstTransactionCommits() throws Exception {

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CountDownLatch firstHoldsLock = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = pool.submit(() -> tx.executeWithoutResult(status -> {
                guard.ensureNotRemovingLastAdmin(repository.findById(adminA).orElseThrow(), false);
                firstHoldsLock.countDown();
                await(releaseFirst);
            }));

            assertTrue(firstHoldsLock.await(10, TimeUnit.SECONDS));

            Future<?> second = pool.submit(() -> tx.executeWithoutResult(status ->
                    guard.ensureNotRemovingLastAdmin(repository.findById(adminB).orElseThrow(), false)));

            assertThrows(TimeoutException.class, () -> second.get(1, TimeUnit.SECONDS),
                    "the second guard call must wait for the first transaction's lock");

            releaseFirst.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void shouldRefuseToRunOutsideATransaction() {

        User admin = repository.findById(adminA).orElseThrow();

        assertThrows(IllegalTransactionStateException.class,
                () -> guard.ensureNotRemovingLastAdmin(admin, false));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
