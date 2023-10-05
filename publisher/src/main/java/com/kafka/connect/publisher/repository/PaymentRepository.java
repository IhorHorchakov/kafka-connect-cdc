package com.kafka.connect.publisher.repository;

import com.kafka.connect.publisher.repository.entity.Payment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface PaymentRepository extends CrudRepository<Payment, Long> {
}
