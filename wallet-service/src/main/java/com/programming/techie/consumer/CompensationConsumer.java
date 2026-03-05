package com.programming.techie.consumer;

import com.programming.techie.events.WalletCompensationEvent;
import com.programming.techie.events.CompensationCompletedEvent;
import com.programming.techie.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class CompensationConsumer {

    private final WalletService walletService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = "wallet-compensation-requested",
            groupId = "wallet-group"
    )
    public void consume(WalletCompensationEvent event) {

        System.out.println("COMPENSATION RECEIVED → " + event.getTransactionId());

        walletService.credit(
                event.getWalletId(),
                event.getAmount()
        );

        // Send confirmation back
        kafkaTemplate.send(
                "compensation-completed",
                event.getTransactionId().toString(),
                new CompensationCompletedEvent(event.getTransactionId())
        );
    }
}
