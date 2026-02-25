package com.programming.techie.controller;

import com.programming.techie.kafka.TestProducer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
    @RequestMapping("/test")
    public class KafkaTestController {

        private final TestProducer producer;

        public KafkaTestController(TestProducer producer) {
            this.producer = producer;
        }

        @GetMapping("/send")
        public String send() {
            producer.sendMessage("Hello Kafka");
            return "Message sent";
        }
    }

