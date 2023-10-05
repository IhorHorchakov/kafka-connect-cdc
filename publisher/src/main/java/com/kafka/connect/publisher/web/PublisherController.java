package com.kafka.connect.publisher.web;

import com.kafka.connect.publisher.repository.entity.Order;
import com.kafka.connect.publisher.repository.entity.Payment;
import com.kafka.connect.publisher.service.OrderService;
import com.kafka.connect.publisher.service.PaymentService;
import com.kafka.connect.publisher.service.dto.OrderDto;
import com.kafka.connect.publisher.service.dto.PaymentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/publisher/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class PublisherController {
    private final PaymentService paymentService;
    private final OrderService orderService;

    @Autowired
    public PublisherController(PaymentService paymentService, OrderService orderService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<Order> postOrder(@RequestBody OrderDto orderDto) {
        Order saved = orderService.save(orderDto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PostMapping("/payments")
    public ResponseEntity<Payment> postPayment(@RequestBody PaymentDto paymentDto) {
        Payment saved = paymentService.save(paymentDto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleException(Throwable throwable) {
        return new ResponseEntity<>(throwable.getLocalizedMessage(), HttpStatus.BAD_REQUEST);
    }
}
