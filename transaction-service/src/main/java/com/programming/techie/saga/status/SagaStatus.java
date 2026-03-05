package com.programming.techie.saga.status;
public enum SagaStatus {
    INITIATED,
    DEBIT_REQUESTED,
    DEBIT_SUCCESS,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}
