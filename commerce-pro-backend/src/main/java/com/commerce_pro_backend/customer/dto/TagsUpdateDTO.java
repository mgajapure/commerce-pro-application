package com.commerce_pro_backend.customer.dto;

import lombok.*;
import java.util.Set;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TagsUpdateDTO {
    private Set<String> tags;
}
