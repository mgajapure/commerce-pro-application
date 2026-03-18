package com.commerce_pro_backend.fulfillment.enums;

public enum PickListStatus {
    GENERATED,    // Created, waiting to be assigned
    ASSIGNED,     // Assigned to a warehouse worker
    IN_PROGRESS,  // Worker has started picking
    COMPLETED,    // All items picked
    CANCELLED     // Pick list voided
}
