package com.commerce_pro_backend.supplier.product.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.supplier.product.entity.SupplierProduct;
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

    public Page<SupplierProduct> getAllSupplierProducts(Pageable pageable) {
        return supplierProductRepository.findAll(pageable);
    }

    @Cacheable(value = "supplier-products", key = "'by-supplier-' + #supplierId")
    public List<SupplierProduct> getBySupplier(String supplierId) {
        return supplierProductRepository.findBySupplierId(supplierId);
    }

    @Cacheable(value = "supplier-products", key = "'by-product-' + #productId")
    public List<SupplierProduct> getByProduct(String productId) {
        return supplierProductRepository.findByProductId(productId);
    }

    public SupplierProduct getSupplierProduct(String id) {
        return supplierProductRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierProduct", id));
    }

    @Transactional
    @CacheEvict(value = "supplier-products", allEntries = true)
    public SupplierProduct createSupplierProduct(SupplierProduct request) {
        log.info("Creating supplier product: supplierId={}, productId={}", request.getSupplierId(), request.getProductId());

        if (supplierProductRepository.existsBySupplierIdAndProductId(request.getSupplierId(), request.getProductId())) {
            throw ApiException.conflict("Supplier product mapping already exists for supplierId: "
                    + request.getSupplierId() + " and productId: " + request.getProductId());
        }

        request.setId(UUID.randomUUID().toString());

        SupplierProduct saved = supplierProductRepository.save(request);
        log.info("Created supplier product with id: {}", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "supplier-products", allEntries = true)
    public SupplierProduct updateSupplierProduct(String id, SupplierProduct request) {
        log.info("Updating supplier product: {}", id);

        SupplierProduct existing = supplierProductRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierProduct", id));

        existing.setSupplierId(request.getSupplierId());
        existing.setSupplierName(request.getSupplierName());
        existing.setProductId(request.getProductId());
        existing.setProductName(request.getProductName());
        existing.setSku(request.getSku());
        existing.setSupplierSku(request.getSupplierSku());
        existing.setUnitCost(request.getUnitCost());
        existing.setCurrency(request.getCurrency());
        existing.setMinOrderQuantity(request.getMinOrderQuantity());
        existing.setLeadTimeDays(request.getLeadTimeDays());
        existing.setIsActive(request.getIsActive());
        existing.setIsPrimary(request.getIsPrimary());
        existing.setNotes(request.getNotes());

        SupplierProduct updated = supplierProductRepository.save(existing);
        log.info("Updated supplier product with id: {}", updated.getId());
        return updated;
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
