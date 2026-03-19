package com.commerce_pro_backend.customer.enums;
/**
 * REGULAR  → default
 * SILVER   → ≥ $500  lifetime spend OR ≥  5 orders
 * GOLD     → ≥ $2000 lifetime spend OR ≥ 20 orders
 * PLATINUM → ≥ $10000 lifetime spend OR ≥ 50 orders
 */
public enum CustomerTier { REGULAR, SILVER, GOLD, PLATINUM }
