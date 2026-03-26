package com.commerce_pro_backend.supplier.purchase_order.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.supplier.purchase_order.entity.PurchaseOrder;
import com.commerce_pro_backend.supplier.purchase_order.entity.PurchaseOrderItem;
import com.commerce_pro_backend.supplier.purchase_order.repository.PurchaseOrderItemRepository;
import com.commerce_pro_backend.supplier.purchase_order.repository.PurchaseOrderRepository;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final CurrentUserService currentUserService;

    public Page<PurchaseOrder> getAllPurchaseOrders(Pageable pageable) {
        return purchaseOrderRepository.findAll(pageable);
    }

    public List<PurchaseOrder> getBySupplier(String supplierId) {
        return purchaseOrderRepository.findBySupplierIdOrderByOrderDateDesc(supplierId);
    }

    public List<PurchaseOrder> getByStatus(String status) {
        return purchaseOrderRepository.findByStatus(status);
    }

    public PurchaseOrder getPurchaseOrder(String id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PurchaseOrder", id));
    }

    @Transactional
    @CacheEvict(value = "purchase-orders", allEntries = true)
    public PurchaseOrder createPurchaseOrder(PurchaseOrder request) {
        log.info("Creating purchase order for supplier: {}", request.getSupplierId());

        request.setId(UUID.randomUUID().toString());
        request.setPoNumber(generatePoNumber());
        request.setCreatedBy(currentUserService.getCurrentUserId());
        request.setOrderDate(Instant.now());

        calculateTotals(request);

        PurchaseOrder saved = purchaseOrderRepository.save(request);
        log.info("Created purchase order with id: {}, poNumber: {}", saved.getId(), saved.getPoNumber());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "purchase-orders", allEntries = true)
    public PurchaseOrder updatePurchaseOrder(String id, PurchaseOrder request) {
        log.info("Updating purchase order: {}", id);

        PurchaseOrder existing = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PurchaseOrder", id));

        existing.setSupplierId(request.getSupplierId());
        existing.setSupplierName(request.getSupplierName());
        existing.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        existing.setCurrency(request.getCurrency());
        existing.setNotes(request.getNotes());
        existing.setTaxAmount(request.getTaxAmount());
        existing.setShippingCost(request.getShippingCost());

        // Update items
        existing.getItems().clear();
        if (request.getItems() != null) {
            for (PurchaseOrderItem item : request.getItems()) {
                item.setId(UUID.randomUUID().toString());
                existing.addItem(item);
            }
        }

        calculateTotals(existing);

        PurchaseOrder updated = purchaseOrderRepository.save(existing);
        log.info("Updated purchase order with id: {}", updated.getId());
        return updated;
    }

    @Transactional
    @CacheEvict(value = "purchase-orders", allEntries = true)
    public void deletePurchaseOrder(String id) {
        log.info("Deleting purchase order: {}", id);
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PurchaseOrder", id));

        if (!"DRAFT".equals(purchaseOrder.getStatus())) {
            throw ApiException.badRequest("Can only delete purchase orders in DRAFT status");
        }

        purchaseOrderRepository.delete(purchaseOrder);
        log.info("Deleted purchase order with id: {}", id);
    }

    @Transactional
    @CacheEvict(value = "purchase-orders", allEntries = true)
    public PurchaseOrder updateStatus(String id, String status) {
        log.info("Updating purchase order status: id={}, status={}", id, status);

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PurchaseOrder", id));

        purchaseOrder.setStatus(status);

        if ("APPROVED".equals(status)) {
            purchaseOrder.setApprovedBy(currentUserService.getCurrentUserId());
            purchaseOrder.setApprovedAt(Instant.now());
        }

        PurchaseOrder updated = purchaseOrderRepository.save(purchaseOrder);
        log.info("Updated purchase order status: id={}, newStatus={}", id, status);
        return updated;
    }

    @Transactional
    @CacheEvict(value = "purchase-orders", allEntries = true)
    public PurchaseOrder receivePurchaseOrder(String id) {
        log.info("Receiving purchase order: {}", id);

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PurchaseOrder", id));

        for (PurchaseOrderItem item : purchaseOrder.getItems()) {
            item.setQuantityReceived(item.getQuantityOrdered());
        }

        purchaseOrder.setStatus("RECEIVED");
        purchaseOrder.setActualDeliveryDate(Instant.now());

        PurchaseOrder updated = purchaseOrderRepository.save(purchaseOrder);
        log.info("Received purchase order with id: {}", id);
        return updated;
    }

    public Map<String, Object> getPurchaseOrderStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("draft", purchaseOrderRepository.countByStatus("DRAFT"));
        stats.put("submitted", purchaseOrderRepository.countByStatus("SUBMITTED"));
        stats.put("approved", purchaseOrderRepository.countByStatus("APPROVED"));
        stats.put("received", purchaseOrderRepository.countByStatus("RECEIVED"));
        stats.put("cancelled", purchaseOrderRepository.countByStatus("CANCELLED"));
        stats.put("total", purchaseOrderRepository.count());

        BigDecimal totalValue = purchaseOrderRepository.sumTotalAmountByStatus("APPROVED")
                .add(purchaseOrderRepository.sumTotalAmountByStatus("RECEIVED"));
        stats.put("totalValue", totalValue);

        return stats;
    }

    private String generatePoNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        String poNumber = "PO-" + datePart + "-" + randomPart;

        while (purchaseOrderRepository.existsByPoNumber(poNumber)) {
            randomPart = String.format("%04d", (int) (Math.random() * 10000));
            poNumber = "PO-" + datePart + "-" + randomPart;
        }

        return poNumber;
    }

    private void calculateTotals(PurchaseOrder purchaseOrder) {
        BigDecimal subtotal = BigDecimal.ZERO;

        if (purchaseOrder.getItems() != null) {
            for (PurchaseOrderItem item : purchaseOrder.getItems()) {
                if (item.getUnitCost() != null && item.getQuantityOrdered() != null) {
                    BigDecimal itemTotal = item.getUnitCost().multiply(BigDecimal.valueOf(item.getQuantityOrdered()));
                    item.setTotalCost(itemTotal);
                    subtotal = subtotal.add(itemTotal);
                }
            }
        }

        purchaseOrder.setSubtotal(subtotal);

        BigDecimal taxAmount = purchaseOrder.getTaxAmount() != null ? purchaseOrder.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal shippingCost = purchaseOrder.getShippingCost() != null ? purchaseOrder.getShippingCost() : BigDecimal.ZERO;
        purchaseOrder.setTotalAmount(subtotal.add(taxAmount).add(shippingCost));
    }
}
