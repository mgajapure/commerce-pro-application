package com.commerce_pro_backend.fulfillment.enums;

public enum PickListType {
    SINGLE,  // One order per list
    BATCH,   // Multiple orders in one run
    ZONE,    // Picks grouped by warehouse zone
    WAVE     // Scheduled wave picking
}
