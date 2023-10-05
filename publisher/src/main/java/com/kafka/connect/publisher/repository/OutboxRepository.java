package com.kafka.connect.publisher.repository;

import com.kafka.connect.publisher.repository.entity.OutboxRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface OutboxRepository extends CrudRepository<OutboxRecord, Long> {
}
