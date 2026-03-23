package com.commerce_pro_backend.ai.repository;

import com.commerce_pro_backend.ai.entity.AiConfig;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiConfigRepository extends JpaRepository<AiConfig, String> {

    Optional<AiConfig> findByFeatureType(AiFeatureType featureType);
}
