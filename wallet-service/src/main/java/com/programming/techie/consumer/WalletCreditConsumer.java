package com.programming.techie.consumer;

import com.programming.techie.events.WalletCreditEvent;
import com.programming.techie.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletCreditConsumer {

    private final WalletService walletService;

    @KafkaListener(
            topics = "wallet-credit",
            groupId = "wallet-group"
    )
    public void consume(WalletCreditEvent event) {

        System.out.println("CREDIT EVENT RECEIVED → " + event.getWalletId());

        walletService.credit(
                event.getWalletId(),
                event.getAmount()
        );
    }
}