package com.programming.techie.agent.config;

import com.programming.techie.agent.tools.*;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ToolConfiguration — registers all AI tools with Spring AI's tool registry.
 *
 * Designed to be extensible: register scheduled, recurring, and conditional tools.
 */
@Configuration
public class ToolConfiguration {

    @Bean
    public ToolCallback[] financialTools(BalanceTool balanceTool,
                                         TransferMoneyTool transferMoneyTool,
                                         TransactionHistoryTool transactionHistoryTool,
                                         SchedulePaymentTool schedulePaymentTool,
                                         RecurringPaymentTool recurringPaymentTool,
                                         ConditionalTriggerTool conditionalTriggerTool) {
        
        return ToolCallbacks.from(
                balanceTool,
                transferMoneyTool,
                transactionHistoryTool,
                schedulePaymentTool,
                recurringPaymentTool,
                conditionalTriggerTool
        );
    }
}

