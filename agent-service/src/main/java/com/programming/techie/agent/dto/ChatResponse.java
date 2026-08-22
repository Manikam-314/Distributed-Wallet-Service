package com.programming.techie.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String response;
    private String tool;
    private String status;
    private String reason;
    private String executionTime;
    private String requestId;
    private boolean confirmationRequired;
}
