package com.programming.techie.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationEvent {
    private String mobileNumber;
    private String email;
    private String message;
    private String deliveryChannel; // "SMS", "EMAIL", "BOTH"
}
