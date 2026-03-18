package com.commerce_pro_backend.fulfillment.repository;

import com.commerce_pro_backend.fulfillment.entity.Carrier;
import com.commerce_pro_backend.fulfillment.enums.CarrierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier, String> {

    Optional<Carrier> findByCode(String code);

    boolean existsByCode(String code);

    List<Carrier> findByStatus(CarrierStatus status);

    Optional<Carrier> findByIsDefaultTrue();
}
