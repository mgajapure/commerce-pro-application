package com.commerce_pro_backend.supplier.directory.mapper;

import com.commerce_pro_backend.supplier.directory.dto.SupplierDto;
import com.commerce_pro_backend.supplier.directory.entity.Supplier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class SupplierMapper {

    public Supplier toEntity(SupplierDto.Request dto) {
        if (dto == null) {
            return null;
        }

        return Supplier.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .contactPerson(dto.getContactPerson())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .country(dto.getCountry())
                .postalCode(dto.getPostalCode())
                .website(dto.getWebsite())
                .description(dto.getDescription())
                .paymentTerms(dto.getPaymentTerms())
                .paymentTermsDays(dto.getPaymentTermsDays())
                .leadTimeDays(dto.getLeadTimeDays())
                .minOrderValue(dto.getMinOrderValue())
                .preferredCurrency(dto.getPreferredCurrency() != null ? dto.getPreferredCurrency() : "USD")
                .certifications(dto.getCertifications())
                .notes(dto.getNotes())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .isPreferred(dto.getIsPreferred() != null ? dto.getIsPreferred() : false)
                .productCount(0)
                .build();
    }

    public SupplierDto.Response toResponse(Supplier entity) {
        if (entity == null) {
            return null;
        }

        return SupplierDto.Response.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .contactPerson(entity.getContactPerson())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .address(entity.getAddress())
                .city(entity.getCity())
                .state(entity.getState())
                .country(entity.getCountry())
                .postalCode(entity.getPostalCode())
                .website(entity.getWebsite())
                .description(entity.getDescription())
                .paymentTerms(entity.getPaymentTerms())
                .paymentTermsDays(entity.getPaymentTermsDays())
                .leadTimeDays(entity.getLeadTimeDays())
                .rating(entity.getRating())
                .minOrderValue(entity.getMinOrderValue())
                .preferredCurrency(entity.getPreferredCurrency())
                .certifications(entity.getCertifications())
                .notes(entity.getNotes())
                .isActive(entity.getIsActive())
                .isPreferred(entity.getIsPreferred())
                .productCount(entity.getProductCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SupplierDto.ListResponse toListResponse(Supplier entity) {
        if (entity == null) {
            return null;
        }

        return SupplierDto.ListResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .contactPerson(entity.getContactPerson())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .rating(entity.getRating())
                .isActive(entity.getIsActive())
                .isPreferred(entity.getIsPreferred())
                .productCount(entity.getProductCount())
                .build();
    }

    public List<SupplierDto.ListResponse> toListResponseList(List<Supplier> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(this::toListResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(SupplierDto.Request dto, Supplier entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getCode() != null) {
            entity.setCode(dto.getCode());
        }
        if (dto.getContactPerson() != null) {
            entity.setContactPerson(dto.getContactPerson());
        }
        if (dto.getEmail() != null) {
            entity.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            entity.setPhone(dto.getPhone());
        }
        if (dto.getAddress() != null) {
            entity.setAddress(dto.getAddress());
        }
        if (dto.getCity() != null) {
            entity.setCity(dto.getCity());
        }
        if (dto.getState() != null) {
            entity.setState(dto.getState());
        }
        if (dto.getCountry() != null) {
            entity.setCountry(dto.getCountry());
        }
        if (dto.getPostalCode() != null) {
            entity.setPostalCode(dto.getPostalCode());
        }
        if (dto.getWebsite() != null) {
            entity.setWebsite(dto.getWebsite());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getPaymentTerms() != null) {
            entity.setPaymentTerms(dto.getPaymentTerms());
        }
        if (dto.getPaymentTermsDays() != null) {
            entity.setPaymentTermsDays(dto.getPaymentTermsDays());
        }
        if (dto.getLeadTimeDays() != null) {
            entity.setLeadTimeDays(dto.getLeadTimeDays());
        }
        if (dto.getMinOrderValue() != null) {
            entity.setMinOrderValue(dto.getMinOrderValue());
        }
        if (dto.getPreferredCurrency() != null) {
            entity.setPreferredCurrency(dto.getPreferredCurrency());
        }
        if (dto.getCertifications() != null) {
            entity.setCertifications(dto.getCertifications());
        }
        if (dto.getNotes() != null) {
            entity.setNotes(dto.getNotes());
        }
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
        if (dto.getIsPreferred() != null) {
            entity.setIsPreferred(dto.getIsPreferred());
        }
    }
}
