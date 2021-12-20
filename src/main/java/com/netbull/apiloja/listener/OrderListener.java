package com.netbull.apiloja.listener;

import com.netbull.apiloja.domain.order.Order;
import com.netbull.apiloja.domain.order.OrderRepository;
import com.netbull.apiloja.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderListener {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderService orderService;

    @RabbitListener(queues = "order-store-created")
    public void executeCreate(Order order) {
        orderService.persistOrder(order);
    }

    @RabbitListener(queues = "order-store-updated-delivered")
    public void executeUpdate(Order order) {
        Order otherOrder = orderRepository.findById(order.getId()).get();
        otherOrder.setState(order.getState());
        otherOrder.setOrderDelivered(order.getOrderDelivered());

        if (orderRepository.save(order) != null) {
            log.info("Endereço alterado: {}", order.getState());
        }
    }
}