package com.programming.techie.service;

import com.programming.techie.events.TransactionProcessedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.programming.techie.entity.LedgerEntry;
import com.programming.techie.entity.WalletEntity;
import com.programming.techie.entity.CompensationLog;
import com.programming.techie.repository.LedgerRepository;
//import com.programming.techie.repository.TransactionRepository;
import com.programming.techie.repository.WalletRepository;
import com.programming.techie.repository.CompensationLogRepository;
import com.programming.techie.repository.IdempotencyRecordRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import com.programming.techie.entity.WalletSnapshot;
import com.programming.techie.repository.WalletSnapshotRepository;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final WalletSnapshotRepository snapshotRepository;
    private final CompensationLogRepository compensationLogRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FraudDetectionService fraudDetectionService;
    private final RedisLockService redisLockService;
    private final RateLimitService rateLimitService;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    public WalletService(
            WalletRepository walletRepository,
            LedgerRepository ledgerRepository,
            WalletSnapshotRepository snapshotRepository,
            CompensationLogRepository compensationLogRepository,
            IdempotencyRecordRepository idempotencyRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            FraudDetectionService fraudDetectionService,
            RedisLockService redisLockService,
            RateLimitService rateLimitService,
            RedisTemplate<String, Object> redisTemplate) {

        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.snapshotRepository = snapshotRepository;
        this.compensationLogRepository = compensationLogRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.fraudDetectionService = fraudDetectionService;
        this.redisLockService = redisLockService;
        this.rateLimitService = rateLimitService;
        this.redisTemplate = redisTemplate;
    }

    // ================= CREATE WALLET =================
    public void createWallet(Long userId) {
        walletRepository.findFirstByUserId(userId).ifPresentOrElse(
            existing -> {
                log.info("Wallet already exists for userId: {}. Idempotency check passed.", userId);
            },
            () -> {
                try {
                    WalletEntity wallet = new WalletEntity();
                    wallet.setUserId(userId);
                    wallet.setBalance(java.math.BigDecimal.ZERO);
                    walletRepository.save(wallet);
                    log.info("Wallet successfully created for userId: {}", userId);
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    log.info("Wallet already created by another process concurrently for userId: {}. Creation skipped.", userId);
                }
            }
        );
    }

    public WalletEntity getWalletByUserId(Long userId) {
        return walletRepository.findFirstByUserId(userId)
                .orElseGet(() -> {
                    try {
                        log.warn("Wallet not found for user {}, resiliently healing state by creating one lazily.", userId);
                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.setBalance(java.math.BigDecimal.ZERO);
                        return walletRepository.save(wallet);
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        log.info("Wallet was created by another process concurrently for user {}. Fetching it now.", userId);
                        return walletRepository.findFirstByUserId(userId)
                                .orElseThrow(() -> new RuntimeException("Concurrent wallet creation failed to resolve for userId: " + userId));
                    }
                });
    }

    public WalletEntity getWalletById(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));
    }

    @Transactional
    public java.math.BigDecimal deposit(Long walletId, java.math.BigDecimal amount) {

        log.info("Deposit request received → walletId={}, amount={}", walletId, amount);

        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Update Cache Proactively Atomically
        updateBalanceCacheAtomic(walletId, amount);

        // Optional: Trigger Snapshot every 10 events
        checkAndCreateSnapshot(walletId);

        return wallet.getBalance();
    }

    // ⭐ NEW → DEBIT WALLET
    @Transactional
    @Bulkhead(name = "walletService")
    @RateLimiter(name = "walletService")
    @TimeLimiter(name = "walletService")
    public void debit(Long walletId, java.math.BigDecimal amount) {
        log.info("Debit request received → walletId={}, amount={}", walletId, amount);

        rateLimitService.checkRateLimit(walletId);
        fraudDetectionService.checkFraud(walletId);

        String lockValue = redisLockService.acquireLock(walletId);
        if (lockValue == null) {
            throw new RuntimeException("Wallet is busy. Try again.");
        }

        try {
            WalletEntity wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            LedgerEntry entry = new LedgerEntry();
            entry.setWalletId(walletId);
            entry.setAmount(amount.negate());
            entry.setType("DEBIT");

            ledgerRepository.save(entry);

            wallet.setBalance(wallet.getBalance().subtract(amount));
            walletRepository.save(wallet);

            // Update Cache Proactively Atomically
            updateBalanceCacheAtomic(walletId, amount.negate());

            checkAndCreateSnapshot(walletId);

        } finally {
            redisLockService.releaseLock(walletId, lockValue);
        }
    }

    @Transactional
    public void processTransaction(Long transactionId, Long walletId, BigDecimal amount) {
        try {
            debit(walletId, amount);
            log.info("SENDING SUCCESS EVENT → {}", transactionId);
            kafkaTemplate.send(
                    "transaction-processed",
                    transactionId.toString(),
                    new TransactionProcessedEvent(transactionId, "SUCCESS", null));

        } catch (Exception e) {
            log.error("SENDING FAILED EVENT → {}", transactionId);
            kafkaTemplate.send(
                    "transaction-processed",
                    transactionId.toString(),
                    new TransactionProcessedEvent(transactionId, "FAILED", e.getMessage()));
            throw e;
        }
    }

    // ⭐ NEW → CREDIT WALLET
    @Transactional
    public void credit(Long transactionId, Long walletId, BigDecimal amount) {
        credit(transactionId, walletId, amount, "CREDIT");
    }

    @Transactional
    public void credit(Long transactionId, Long walletId, java.math.BigDecimal amount, String type) {

        log.info("Credit request received → txId={}, walletId={}, amount={}, type={}", transactionId, walletId, amount,
                type);

        // Idempotency check
        if (transactionId != null) {
            String idempKey = "CREDIT_" + transactionId;
            if (idempotencyRepository.existsById(idempKey)) {
                log.warn("Credit already processed for txId={}", transactionId);
                return;
            }
            idempotencyRepository.save(
                    new com.programming.techie.entity.IdempotencyRecord(idempKey, "wallet-service", Instant.now()));
        }

        LedgerEntry entry = new LedgerEntry();
        entry.setWalletId(walletId);
        entry.setAmount(amount);
        entry.setType(type);

        ledgerRepository.save(entry);

        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Update Cache Proactively Atomically
        updateBalanceCacheAtomic(walletId, amount);

        checkAndCreateSnapshot(walletId);
    }

    @Transactional
    public void compensate(Long transactionId, Long walletId, BigDecimal amount) {
        log.info("Compensating transaction {} for wallet {}", transactionId, walletId);

        if (compensationLogRepository.findByTransactionId(transactionId).isPresent()) {
            log.warn("Compensation already processed for transaction {}", transactionId);
            return;
        }

        credit(transactionId, walletId, amount, "COMPENSATION");
    }

    private void updateBalanceCacheAtomic(Long walletId, BigDecimal amountToAdd) {
        try {
            String key = "wallet:balance::" + walletId;
            // Option A: Redis increment atomic operation. Works flawlessly.
            // In spring data redis, increment takes double/long. Balance is BigDecimal.
            // Safe increment:
            BigDecimal currentBigDecimal = BigDecimal.ZERO;
            Object existing = redisTemplate.opsForValue().get(key);
            if (existing != null) {
                if (existing instanceof java.math.BigDecimal)
                    currentBigDecimal = (java.math.BigDecimal) existing;
                else if (existing instanceof Double)
                    currentBigDecimal = BigDecimal.valueOf((Double) existing);
                else if (existing instanceof Integer)
                    currentBigDecimal = BigDecimal.valueOf((Integer) existing);
                else if (existing instanceof String)
                    currentBigDecimal = new BigDecimal((String) existing);
            }

            BigDecimal newVal = currentBigDecimal.add(amountToAdd);
            redisTemplate.opsForValue().set(key, newVal);
            log.info("Updated balance cache (atomic fallback) for wallet {} to {}", walletId, newVal);
        } catch (Exception e) {
            log.error("Failed to update balance cache for wallet {}", walletId, e);
        }
    }

    // ================= BALANCE =================
    @Cacheable(value = "wallet:balance", key = "#walletId")
    public java.math.BigDecimal getBalance(Long walletId) {
        log.info("Fetching balance from DB for walletId: {}", walletId);
        return walletRepository.findById(walletId)
                .map(WalletEntity::getBalance)
                .orElse(java.math.BigDecimal.ZERO);
    }

    // ================= SNAPSHOTTING & REPLAY =================

    private void checkAndCreateSnapshot(Long walletId) {
        long eventCount = ledgerRepository.countByWalletId(walletId);
        if (eventCount % 10 == 0) { // Simple threshold: snapshot every 10 events
            log.info("Triggering snapshot for walletId: {}", walletId);
            BigDecimal currentBalance = ledgerRepository.calculateBalance(walletId);

            // Get the last event ID for this wallet
            Long lastEventId = ledgerRepository.findFirstByWalletIdOrderByIdDesc(walletId)
                    .map(LedgerEntry::getId)
                    .orElse(0L);

            WalletSnapshot snapshot = WalletSnapshot.builder()
                    .walletId(walletId)
                    .balance(currentBalance)
                    .lastEventId(lastEventId)
                    .build();
            snapshotRepository.save(snapshot);
        }
    }

    public BigDecimal rebuildBalanceFromEvents(Long walletId) {
        log.info("Rebuilding balance from events/snapshots for walletId: {}", walletId);

        return snapshotRepository.findFirstByWalletIdOrderByLastEventIdDesc(walletId)
                .map(snapshot -> {
                    BigDecimal baseBalance = snapshot.getBalance();
                    BigDecimal delta = ledgerRepository.calculateBalanceFromId(walletId, snapshot.getLastEventId());
                    return baseBalance.add(delta);
                })
                .orElseGet(() -> ledgerRepository.calculateBalance(walletId));
    }
}
