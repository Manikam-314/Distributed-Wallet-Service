package com.programming.techie.scheduler;

import com.programming.techie.client.AuthClient;
import com.programming.techie.dto.UserDTO;
import com.programming.techie.entity.ScheduledPayment;
import com.programming.techie.repository.RecurringPaymentRepository;
import com.programming.techie.repository.ScheduledPaymentRepository;
import com.programming.techie.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentFeaturesSchedulerTest {

    @Mock
    private ScheduledPaymentRepository scheduledPaymentRepository;

    @Mock
    private RecurringPaymentRepository recurringPaymentRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private AuthClient authClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private AgentFeaturesScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AgentFeaturesScheduler(
                scheduledPaymentRepository,
                recurringPaymentRepository,
                transactionService,
                authClient,
                kafkaTemplate
        );
    }

    @Test
    void testProcessScheduledPayments_conditionMet() {
        ScheduledPayment payment = ScheduledPayment.builder()
                .id(1L)
                .userId(100L)
                .senderWalletId(10L)
                .receiverWalletId(20L)
                .amount(BigDecimal.valueOf(1000))
                .status("PENDING")
                .executionTime(LocalDateTime.now().minusMinutes(5))
                .conditionMinAmount(BigDecimal.valueOf(20000))
                .conditionMet(true)
                .build();

        when(scheduledPaymentRepository.findByStatusAndExecutionTimeLessThanEqual(eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(payment));

        scheduler.sweepAgentFeatures();

        // Verify transfer was executed
        verify(transactionService, times(1)).transfer(
                eq(10L), eq(20L), eq(BigDecimal.valueOf(1000)), eq("SCH-PAY-1"), eq(100L)
        );

        // Verify status marked completed and saved
        assertEquals("COMPLETED", payment.getStatus());
        verify(scheduledPaymentRepository, times(1)).save(payment);
    }

    @Test
    void testProcessScheduledPayments_conditionNotMet_triggersReminder() {
        ScheduledPayment payment = ScheduledPayment.builder()
                .id(2L)
                .userId(100L)
                .senderWalletId(10L)
                .receiverWalletId(20L)
                .amount(BigDecimal.valueOf(1000))
                .status("PENDING")
                .executionTime(LocalDateTime.now().minusMinutes(5))
                .conditionMinAmount(BigDecimal.valueOf(20000))
                .conditionMet(false)
                .reminderMessage("Salary not credited!")
                .build();

        UserDTO user = new UserDTO();
        user.setId(100L);
        user.setEmail("user@example.com");
        user.setMobileNumber("1234567890");
        user.setName("Test User");

        when(scheduledPaymentRepository.findByStatusAndExecutionTimeLessThanEqual(eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(payment));
        when(authClient.getUser(100L)).thenReturn(user);

        scheduler.sweepAgentFeatures();

        // Verify transfer was NOT executed
        verify(transactionService, never()).transfer(anyLong(), anyLong(), any(BigDecimal.class), anyString(), anyLong());

        // Verify notification was sent
        verify(kafkaTemplate, times(1)).send(eq("notificationTopic"), any());

        // Verify status marked reminded
        assertEquals("REMINDED", payment.getStatus());
        verify(scheduledPaymentRepository, times(1)).save(payment);
    }
}
