package com.commerce_pro_backend.supplier.product.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.supplier.product.dto.SupplierProductDto;
import com.commerce_pro_backend.supplier.product.entity.SupplierProduct;
import com.commerce_pro_backend.supplier.product.mapper.SupplierProductMapper;
import com.commerce_pro_backend.supplier.product.repository.SupplierProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SupplierProductService {

    private final SupplierProductRepository supplierProductRepository;
    private final SupplierProductMapper supplierProductMapper;

    public Page<SupplierProductDto.ListResponse> getAllSupplierProducts(Pageable pageable) {
        return supplierProductRepository.findAll(pageable)
                .map(supplierProductMapper::toListResponse);
    }

    @Cacheable(value = "supplier-products", key = "'by-supplier-' + #supplierId")
    public List<SupplierProductDto.ListResponse> getBySupplier(String supplierId) {
        return supplierProductMapper.toListResponseList(
                supplierProductRepository.findBySupplierId(supplierId));
    }

    @Cacheable(value = "supplier-products", key = "'by-product-' + #productId")
    public List<SupplierProductDto.ListResponse> getByProduct(String productId) {
        return supplierProductMapper.toListResponseList(
                supplierProductRepository.findByProductId(productId));
    }

    public SupplierProductDto.Response getSupplierProduct(String id) {
        SupplierProduct supplierProduct = supplierProductRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierProduct", id));
        return supplierProductMapper.toResponse(supplierProduct);
    }

    @Transactional
    @CacheEvict(value = "supplier-products", allEntries = true)
    public SupplierProductDto.Response createSupplierProduct(SupplierProductDto.Request request) {
        log.info("Creating supplier product: supplierId={}, productId={}", request.getSupplierId(), request.getProductId());

        if (supplierProductRepository.existsBySupplierIdAndProductId(request.getSupplierId(), request.getProductId())) {
            throw ApiException.conflict("Supplier product mapping already exists for supplierId: "
                    + request.getSupplierId() + " and productId: " + request.getProductId());
        }

        SupplierProduct entity = supplierProductMapper.toEntity(request);
        entity.setId(UUID.randomUUID().toString());

        SupplierProduct saved = supplierProductRepository.save(entity);
        log.info("Created supplier product with id: {}", saved.getId());
        return supplierProductMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "supplier-products", allEntries = true)
    public SupplierProductDto.Response updateSupplierProduct(String id, SupplierProductDto.Request request) {
        log.info("Updating supplier product: {}", id);

        SupplierProduct existing = supplierProductRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierProduct", id));

        supplierProductMapper.updateEntityFromDto(request, existing);

        SupplierProduct updated = supplierProductRepository.save(existing);
        log.info("Updated supplier product with id: {}", updated.getId());
        return supplierProductMapper.toResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "supplier-products", allEntries = true)
    public void deleteSupplierProduct(String id) {
        log.info("Deleting supplier product: {}", id);
        SupplierProduct supplierProduct = supplierProductRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierProduct", id));
        supplierProductRepository.delete(supplierProduct);
        log.info("Deleted supplier product with id: {}", id);
    }
}
