package com.commerce_pro_backend.catalog.collection.repository;

import com.commerce_pro_backend.catalog.collection.entity.Collection;
import com.commerce_pro_backend.catalog.collection.entity.Collection.CollectionStatus;
import com.commerce_pro_backend.catalog.collection.entity.Collection.CollectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, String>, JpaSpecificationExecutor<Collection> {

    Optional<Collection> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Collection> findByIsFeaturedTrueAndStatusOrderBySortOrderAsc(CollectionStatus status);

    long countByStatus(CollectionStatus status);

    long countByType(CollectionType type);

    long countByIsFeaturedTrue();
}
