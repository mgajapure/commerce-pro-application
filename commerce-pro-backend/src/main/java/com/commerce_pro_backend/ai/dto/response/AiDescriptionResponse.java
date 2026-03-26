package com.commerce_pro_backend.ai.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiDescriptionResponse {
    private String productId;
    private String description;
    private String shortDescription;
    private boolean saved;
}
