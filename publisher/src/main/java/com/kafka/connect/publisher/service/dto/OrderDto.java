package com.kafka.connect.publisher.service.dto;

import com.kafka.connect.publisher.repository.entity.Order;

public class OrderDto {
    private int quantity;
    private Order.DeliveryMethod deliveryMethod;

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Order.DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(Order.DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }
}
