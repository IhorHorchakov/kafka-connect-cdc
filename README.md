### Kafka Connect: quick overview ###

Kafka Connect works as a centralized data hub for simple data integration between databases, key-value stores, search indexes, and file systems. It provides components just out of the box to configure the integration flow. Many components are free and open-source, and some are paid.

Debezium is a set of source connectors for Apache KafkaConnect. Each connector ingests changes from a different database by using that database’s features for change data capture (CDC). Unlike other approaches, such as polling or dual writes, log-based CDC as implemented by Debezium:
- ensures that all data changes are captured

- produces change events with a very low delay while avoiding increased CPU usage required for frequent polling. For example, for MySQL or PostgreSQL, the delay is in the millisecond range

- can capture deletes

- can capture old record state and additional metadata such as transaction ID and causing query, depending on the database’s capabilities and configuration.

![Generic usage of Kafka Connect](https://github.com/IhorHorchakov/kafka-connect-cdc/blob/master/img/generic_usage_of_kafka_connect.jpg?raw=true)

The components are - Connectors(source/sink), Transformations, and Converters.

**Connectors:** The Connector is a component that coordinates data streaming by managing tasks. A connector instance is a logical job. Each connector instance coordinates a set of tasks that actually copy the data. A connector is run by a worker. Connectors can operate with mostly all famous protocols - JDBC, SFTP, HTTP, AMQP, SMTP, JMS, and HDFS.

A connector consists of multiple stages. For source connectors, KafkaConnect retrieves the records from the connector, applies zero or more transformations, uses the converters to serialize each record’s key, value, and headers, and finally writes each record to Kafka. For sink connectors, KafkaConnect reads the topic(s), uses the converters to deserialize each record’s key, value, and headers, and for each record applies zero or more transformations and delivers the records to the sink connector.

These are famous platforms that provide connectors: ConfluentHub, Debezium, Aiven, and Lenses.

**Transformations(SMTs):**  Using Single Message Transformation we can modify the data as it passes through the KafkaConnect pipeline. For things like manipulating fields, routing messages, conditionally filtering messages, and more, SMT is a perfect solution. If we get to things like aggregation, and joining streams, it looks then SMT may not be the best and we should head over to Kafka Streams instead.
A connector can be configured to run multiple SMTs and execute them in the predefined order.

**Converters:** The Converter performs data serialization from one format to another. They are used to enable compatibility for source and target systems. There are Avro, Protobuf, and JSON converters available.

![Internals of Kafka Connect](https://github.com/IhorHorchakov/kafka-connect-cdc/blob/master/img/internals_of_kafka_connect.jpg?raw=true)

### CDC Outbox pattern: overview

Actually, there are 2 known forms of implementation of the outbox CDC pattern on Postgres.

The first form is to configure Postgres WAL (set wal_level = logical) and set up PostgresConnector to consume the WAL records. No need to create an outbox table. The connector knows what tables to monitor in WAL, reads the WAL, and produces CREATE/UPDATE/DELETE events depending on what the change is. The second approach is to create the outbox table, and set up PostgresConnetor to keep track of the WAL for outbox table only. The outbox table is used typically for inserting records to the end of the table and storing events for future distribution. The connector produces only CREATE events that contain all needed details in the payload. This (second) form is implemented in POC and described below.

### CDC Outbox pattern: implementation using Debezium Postgres connector

There are 4 acting modules configured to run on docker-compose:

![Acting modules](https://github.com/IhorHorchakov/kafka-connect-cdc/blob/master/img/poc_acting_modules.jpg?raw=true)

![DB tables with sample data](https://github.com/IhorHorchakov/kafka-connect-cdc/blob/master/img/poc_db_sample_data.jpg?raw=true)

- Publisher is built around Payment and Order domain entities. It provides REST API methods to post orders and payments to the DB and update the ‘outbox' table accordingly in the same transaction.

- Debezium is a platform for change data capture, it has built-in connectors and other components. Debezium works in the scope of the KafkaConnect cluster. I have configured Debezium to use PostgresConnector to capture data changes from the ‘outbox’ table and push that data to the Apache Kafka cluster.

- Apache Kafka provides 2 topics for CDC events: ‘outbox.event.ORDER' and 'outbox.event.PAYMENT'.

- Consumer has OrderConsumer to receive events from in ‘outbox.event.ORDER’ topic, and PaymentConsumer to listen to the ‘outbox.event.PAYMENT' topic. OrderConsumer and PaymentConsumer just write events to the container’s log.  Kafka topics are created dynamically.

**_To run the project:_**
1) Start up containers from the root of the project: `docker-compose up`

2) Wait until all the docker containers start (~30 seconds)

3) Install Postgres source connector from the project root: `curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" 127.0.0.1:8083/connectors/ --data "@connector/source/postgres-source-connector.json"`

4) Send several requests to publisher API. This will insert your orders and payments to DB, and update the outbox table accordingly:
* `POST: http://localhost:8080/publisher/api/orders`
```json
{
    "quantity": <int>,
    "deliveryMethod": <String:(GRUB_HUB/JUST_EAT/UBER_EATS/DELIVEROO)>
}
```

* `POST: http://localhost:8080/publisher/api/payments`
```json
{
    "orderId": <long>,
    "paymentMethod": <String:(CASH/CREDIT_CARD/MOBILE_PAYMENT)>
}
```
There are no relationships/constraints between Order and Payment entities, so you can put any values to the request body.

6) Tail the logs of the 'consumer' container to see what the Kafka consumer receives from topics:
`docker logs -f --tail 50 kafka-connect-cdc_consumer_1`
<details>
  <summary>Logs from the Consumer container</summary>
    2023-05-07 07:19:11.576  INFO 1 --- [ntainer#0-0-C-1] c.kafka.connect.consumer.OrderConsumer   : Record received from the 'outbox.event.ORDER' topic: [ConsumerRecord(topic = outbox.event.ORDER, partition = 0, leaderEpoch = 0, offset = 1, CreateTime = 1683443951215, serialized key size = 56, serialized value size = 115, headers = RecordHeaders(headers = [RecordHeader(key = id, value = [48, 49, 99, 101, 101, 102, 101, 97, 45, 102, 98, 101, 99, 45, 52, 101, 52, 52, 45, 56, 102, 53, 98, 45, 49, 51, 98, 99, 52, 55, 53, 48, 55, 102, 57, 49])], isReadOnly = false), key = {"schema":{"type":"int64","optional":false},"payload":2}, value = {"schema":{"type":"string","optional":false},"payload":"{\"id\":2,\"quantity\":1,\"deliveryMethod\":\"GRUB_HUB\"}"})]
    2023-05-07 07:19:14.068  INFO 1 --- [ntainer#1-0-C-1] c.k.connect.consumer.PaymentConsumer     : Record received from the 'outbox.event.PAYMENT' topic: [ConsumerRecord(topic = outbox.event.PAYMENT, partition = 0, leaderEpoch = 0, offset = 1, CreateTime = 1683443953496, serialized key size = 56, serialized value size = 139, headers = RecordHeaders(headers = [RecordHeader(key = id, value = [97, 54, 51, 98, 57, 97, 54, 97, 45, 53, 54, 48, 102, 45, 52, 51, 52, 99, 45, 56, 98, 51, 101, 45, 50, 56, 49, 54, 53, 54, 54, 51, 52, 54, 50, 54])], isReadOnly = false), key = {"schema":{"type":"int64","optional":false},"payload":2}, value = {"schema":{"type":"string","optional":false},"payload":"{\"id\":2,\"paymentMethod\":\"CREDIT_CARD\",\"date\":1683443953492,\"orderId\":1}"})]
</details>

7) There is Kafkadrop container running on the port 9000, you can go to `http://localhost:9000` to explore the topics and their messages in details.
8) Grafana is running on `http://localhost:3000` (login - admin, pass - admin). Prometheus is running on `http://localhost:9095`


### Kafka Connect: guarantees ###

When the system is operating normally or being managed carefully then Debezium provides **exactly once** delivery of every change event record. If a fault does happen then the system does not lose any events.

However, when the Debezium’s systems crash, they are not always able to record their last position/offset. When they are restarted, they recover by starting where were last known to have been, and thus the consuming application will always see every event but may likely see at least some messages duplicated during recovery. Additionally, network failures may cause the Debezium connectors not to receive confirmation of writes, resulting in the same event being recorded one or more times (until confirmation is received). In these abnormal situations, Debezium, like Kafka, provides **at least once** delivery of change events.

### Kafka Connect: edge cases ###

_What happens when a monitored database stops or crashes?_

the Debezium connector will retry to establish the connection. Debezium periodically records the connector’s positions and offsets in Kafka, so once the connector establishes communication the connector should continue to read from the last recorded position and offset. The Debezium PostgresConnector externally stores the last processed offset in the form of a PostgreSQL LSN. This offset is available as long as the Debezium replication slot remains intact. Never drop a replication slot on the primary server or you will lose data. (FOR wal_level = logical)

_What if Kafka becomes unavailable?_

as the connector generates change events, the Kafka Connect framework records those events in Kafka by using the Kafka producer API. Periodically (at a frequency that you specify in the configuration), KafkaConnect writes the latest offset that appears in those change events. If the Kafka brokers become unavailable, the KafkaConnect process repeatedly tries to reconnect to the Kafka brokers. In other words, the connector tasks pause until a connection can be re-established, at which point the connectors resume exactly where they left off.

_What if the Connector crashes?_

(case for source connectors) if the Connector process stops unexpectedly, any tasks it was running terminate without recording their most recently processed offsets. When KafkaConnect is being run in distributed mode, it restarts those Connector tasks on other processes. PostgresConnector resumes from the last offset that was recorded by the earlier processes. This means that the new replacement tasks might generate some of the same change events that were processed just prior to the crash. The number of duplicate events depends on the flush period and the number of changes just before the crash.

_What if a Connector is stopped for some time?_

If the connector is gracefully stopped, the database can continue to be used. Changes are recorded in the PostgreSQL WAL. When the connector restarts, it resumes streaming changes where it left off. That is, it generates change event records for all database changes that were made while the connector was stopped. After being stopped for a while, when Connector restarts, it is very likely to catch up with the database changes that were made while it was stopped. How quickly this happens depends on the capabilities and performance of Kafka and the volume of changes being made to the data in PostgreSQL.

_What if the database log contains invalid data (like date-time in an invalid format) that need to be skipped? Or what if it is necessary to reprocess part of the log from the past?_

There is generally no straight way how to achieve this operation but there is a workaround. The workaround is to change the offsets in the source database. This is a highly technical operation manipulating Kafka Connect internals. Please use this only as the last resort solution.

_How does Debezium affect source databases?_

most databases have to be configured before Debezium can monitor them. For example, a MySQL server must be configured to use the row-level binlog, and to have a user privileged to read the binlog; the Debezium connector must be configured with the correct information, including the privileged user. Debezium connectors do not store any information inside the upstream databases. However, running a connector may place an additional load on the source database.