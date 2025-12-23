package io.flamingock.examples.inventory.order;

public record OrderItem(String productId, int quantity, double price) {}
