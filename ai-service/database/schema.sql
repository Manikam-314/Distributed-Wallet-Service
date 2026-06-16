CREATE TABLE IF NOT EXISTS `Reminders` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,            -- UUID
    `senderId` VARCHAR(50) NOT NULL,                  -- User ID who created the reminder
    `receiverId` VARCHAR(50) NOT NULL,                -- User ID who is supposed to receive/pay
    `amount` DECIMAL(15,2),                           -- Amount extracted, NULL if missing
    `dueDate` DATETIME,                               -- Scheduled time
    `message` TEXT NOT NULL,                          -- Original natural language text
    `status` ENUM('PENDING', 'COMPLETED', 'FAILED', 'PROCESSING') DEFAULT 'PENDING',
    `confidence` DECIMAL(5,4),                        -- AI Confidence score (0.0 to 1.0)
    `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX `idx_reminder_status_due` (`status`, `dueDate`) -- Optimized for the Scheduler Worker
);

CREATE TABLE IF NOT EXISTS `FraudEvents` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `transactionId` VARCHAR(50) NOT NULL,
    `userId` VARCHAR(50) NOT NULL,
    `amount` DECIMAL(15,2),
    `fraudScore` DECIMAL(5,4) NOT NULL,               -- The AI Score
    `actionTaken` ENUM('ALLOW', 'FLAG', 'BLOCK') NOT NULL,
    `reason` VARCHAR(255),                            -- Why it blocked/flagged
    `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX `idx_fraud_user` (`userId`)
);
