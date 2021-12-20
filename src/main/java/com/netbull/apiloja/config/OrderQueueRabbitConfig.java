package com.netbull.apiloja.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderQueueRabbitConfig {
    @Bean
    public Exchange orderExchange() {
        return ExchangeBuilder
                .directExchange("order-store")
                .build();
    }


    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder
                .durable("order-store-created")
                .deadLetterExchange("order-store")
                .deadLetterRoutingKey("order.store.deadLetter")
                .deliveryLimit(5)
                .build();
    }

    @Bean
    public Queue orderUpdatedDeliveredQueue() {
        return QueueBuilder
                .durable("order-store-updated-delivered")
                .deadLetterExchange("order-store")
                .deadLetterRoutingKey("order.store.deadLetter")
                .deliveryLimit(5)
                .build();
    }

    @Bean
    public Binding orderCreatedBiding() {
        return BindingBuilder
                .bind(this.orderCreatedQueue())
                .to(this.orderExchange())
                .with("order.store.created")
                .noargs();
    }

    @Bean
    public Binding orderUpdatedDeliveredBiding() {
        return BindingBuilder
                .bind(this.orderUpdatedDeliveredQueue())
                .to(this.orderExchange())
                .with("order.store.updated.delivered")
                .noargs();
    }

    @Bean
    public Queue orderDeadLetterQueue() {
        return QueueBuilder
                .durable("order-store-dead-letter")
                .autoDelete()
                .build();
    }

    @Bean
    public Binding orderDeadLetterBiding() {
        return BindingBuilder
                .bind(this.orderDeadLetterQueue())
                .to(this.orderExchange())
                .with("order.store.deadLetter")
                .noargs();
    }
}