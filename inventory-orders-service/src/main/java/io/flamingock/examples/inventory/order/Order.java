package io.flamingock.examples.inventory.order;

import java.util.List;

public record Order(
        String orderId,
        String customerId,
        List<OrderItem> items,
        double total,
        String status,
        String createdAt,
        String discountCode,
        boolean discountApplied
) {}
