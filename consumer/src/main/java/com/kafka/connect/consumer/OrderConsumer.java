package com.kafka.connect.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderConsumer.class);

    @KafkaListener(groupId = "consumer-group", topics = "outbox.event.ORDER")
    public void listener(ConsumerRecord<?, ?> record) {
        LOGGER.info(String.format("Record received from the 'outbox.event.ORDER' topic: [%s]", record));
    }
}
