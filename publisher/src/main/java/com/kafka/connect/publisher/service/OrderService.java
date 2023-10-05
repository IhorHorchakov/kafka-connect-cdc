package com.kafka.connect.publisher.service;

import com.kafka.connect.publisher.repository.OrderRepository;
import com.kafka.connect.publisher.repository.OutboxRepository;
import com.kafka.connect.publisher.repository.entity.Order;
import com.kafka.connect.publisher.repository.entity.OutboxRecord;
import com.kafka.connect.publisher.service.dto.OrderDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public Order save(OrderDto orderDto) {
        Order order = new Order();
        order.setQuantity(orderDto.getQuantity());
        order.setDeliveryMethod(orderDto.getDeliveryMethod());
        Order savedOrder = orderRepository.save(order);
        saveOutboxRecord(savedOrder.getId(), savedOrder);
        return savedOrder;
    }

    private void saveOutboxRecord(Long orderId, Object recordPayload) {
        OutboxRecord outboxRecord = new OutboxRecord();
        outboxRecord.setId(UUID.randomUUID());
        outboxRecord.setRecordType(OutboxRecord.RecordType.ORDER);
        outboxRecord.setEventId(orderId);
        outboxRecord.setEventType(OutboxRecord.EventType.CREATED);
        outboxRecord.setEventPayload(JsonConverter.convertToJson(recordPayload));
        outboxRecord.setTimestamp(Instant.now());
        outboxRepository.save(outboxRecord);
    }
}
