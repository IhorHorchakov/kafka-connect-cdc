package com.kafka.connect.publisher.service;

import com.kafka.connect.publisher.repository.OutboxRepository;
import com.kafka.connect.publisher.repository.PaymentRepository;
import com.kafka.connect.publisher.repository.entity.OutboxRecord;
import com.kafka.connect.publisher.repository.entity.Payment;
import com.kafka.connect.publisher.service.dto.PaymentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, OutboxRepository outboxRepository) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public Payment save(PaymentDto paymentDto) {
        Payment payment = new Payment();
        payment.setPaymentMethod(paymentDto.getPaymentMethod());
        payment.setDate(new Date());
        payment.setOrderId(paymentDto.getOrderId());
        Payment savedPayment = paymentRepository.save(payment);
        saveOutboxRecord(savedPayment.getId(), savedPayment);
        return payment;
    }

    private void saveOutboxRecord(Long paymentId, Object recordPayload) {
        OutboxRecord outboxRecord = new OutboxRecord();
        outboxRecord.setId(UUID.randomUUID());
        outboxRecord.setRecordType(OutboxRecord.RecordType.PAYMENT);
        outboxRecord.setEventId(paymentId);
        outboxRecord.setEventType(OutboxRecord.EventType.CREATED);
        outboxRecord.setEventPayload(JsonConverter.convertToJson(recordPayload));
        outboxRecord.setTimestamp(Instant.now());
        outboxRepository.save(outboxRecord);
    }
}
