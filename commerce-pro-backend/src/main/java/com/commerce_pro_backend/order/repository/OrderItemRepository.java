package com.commerce_pro_backend.order.repository;

import com.commerce_pro_backend.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    List<OrderItem> findByOrderId(String orderId);

    List<OrderItem> findByProductId(String productId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId AND oi.productId = :productId")
    List<OrderItem> findByOrderIdAndProductId(@Param("orderId") String orderId,
                                              @Param("productId") String productId);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.productId = :productId")
    Long sumQuantityByProductId(@Param("productId") String productId);
    

    // ── Analytics queries ────────────────────────────────────────────────────

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id IN :orderIds")
    List<OrderItem> findByOrderIds(@Param("orderIds") List<String> orderIds);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.order.id IN :orderIds")
    long sumUnitsByOrderIds(@Param("orderIds") List<String> orderIds);

    /**
     * Revenue, units grouped by category (product snapshot category field).
     * Returns Object[]{category, sumLineTotal, sumQuantity}
     */
    /**
     * Returns Object[]{productId, productName, sumLineTotal, sumQuantity}
     * Category must be resolved in the service via ProductRepository.
     */
    @Query("""
        SELECT oi.productId, oi.productName,
               COALESCE(SUM(oi.lineTotal), 0),
               COALESCE(SUM(oi.quantity), 0)
        FROM OrderItem oi
        WHERE oi.order.id IN :orderIds
        GROUP BY oi.productId, oi.productName
        ORDER BY SUM(oi.lineTotal) DESC
        """)
    List<Object[]> sumRevenueByCategory(@Param("orderIds") List<String> orderIds);

    /**
     * Top N products by revenue.
     * Returns Object[]{productId, productName, sku, category, sumQty, sumLineTotal}
     */
    @Query("""
        SELECT oi.productId, oi.productName, oi.sku,
               COALESCE(SUM(oi.quantity), 0),
               COALESCE(SUM(oi.lineTotal), 0)
        FROM OrderItem oi
        WHERE oi.order.id IN :orderIds
        GROUP BY oi.productId, oi.productName, oi.sku
        ORDER BY SUM(oi.lineTotal) DESC
        """)
    List<Object[]> findTopProductsByRevenue(@Param("orderIds") List<String> orderIds,
                                            @Param("topN") int topN);

    /**
     * Margin data per product: productId, productName, sku, category,
     * sumLineTotal, sumCogs (costPrice*qty), sumQty.
     */
    @Query("""
        SELECT oi.productId, oi.productName, oi.sku,
               COALESCE(SUM(oi.lineTotal), 0),
               COALESCE(SUM(CASE WHEN oi.costPrice IS NOT NULL THEN oi.costPrice * oi.quantity ELSE 0 END), 0),
               COALESCE(SUM(oi.quantity), 0)
        FROM OrderItem oi
        WHERE oi.order.id IN :orderIds
        GROUP BY oi.productId, oi.productName, oi.sku
        ORDER BY SUM(oi.lineTotal) DESC
        """)
    List<Object[]> findMarginDataByProduct(@Param("orderIds") List<String> orderIds);

    /**
     * Return data: items where returnedQuantity > 0.
     */
    @Query("""
        SELECT oi FROM OrderItem oi
        WHERE oi.order.id IN :orderIds AND oi.returnedQuantity > 0
        """)
    List<OrderItem> findReturnedItems(@Param("orderIds") List<String> orderIds);
}
