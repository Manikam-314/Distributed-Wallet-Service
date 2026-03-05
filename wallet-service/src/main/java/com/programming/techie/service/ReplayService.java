package com.programming.techie.service;

import com.programming.techie.entity.FailedEvent;
import com.programming.techie.producer.ReplayProducer;
import com.programming.techie.repository.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReplayService {

    private final FailedEventRepository failedEventRepository;
    private final ReplayProducer replayProducer;

    public void replayFailedEvent(Long id) {

        FailedEvent event = failedEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Failed event not found"));

        replayProducer.replay(
                event.getOriginalTopic().replace(".DLT", ""),
                event.getPayload()
        );

        // optional: delete after replay
        failedEventRepository.delete(event);

        System.out.println("Replay completed for event: " + id);
    }
}
