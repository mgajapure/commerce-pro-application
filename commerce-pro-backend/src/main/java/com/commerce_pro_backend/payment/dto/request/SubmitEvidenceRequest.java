package com.commerce_pro_backend.payment.dto.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class SubmitEvidenceRequest {
    @NotBlank private String evidenceNotes;
    private List<String> evidenceFilePaths;

    public String getEvidenceFilePathsJson() {
        try {
            return new ObjectMapper().writeValueAsString(evidenceFilePaths);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
