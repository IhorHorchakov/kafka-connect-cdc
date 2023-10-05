package com.kafka.connect.publisher.service.dto;

import com.kafka.connect.publisher.repository.entity.Payment;

public class PaymentDto {
    private Payment.PaymentMethod paymentMethod;

    private Long orderId;

    public Payment.PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Payment.PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
