package com.commerce_pro_backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiCardDTO {

    private String title;
    private String value;
    private double change;
    private double changePercent;
    private Trend trend;
    private String period;

    public enum Trend {
        UP, DOWN, STABLE
    }
}
