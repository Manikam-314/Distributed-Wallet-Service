package com.programming.techie.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

// Twilio is initialized via SmsService (using REST API directly - no SDK needed)
@Configuration
@Slf4j
public class TwilioConfig {
    // No-op: credentials are injected directly into SmsService
}
