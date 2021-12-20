package com.netbull.apiloja.service;

import com.netbull.apiloja.domain.order.Order;
import com.netbull.apiloja.domain.order.OrderRepository;
import com.netbull.apiloja.domain.order.OrderState;
import com.netbull.apiloja.domain.order.addressClient.AddressClient;
import com.netbull.apiloja.domain.order.client.Client;
import com.netbull.apiloja.domain.order.product.ProductOrder;
import com.netbull.apiloja.domain.product.Product;
import com.netbull.apiloja.domain.product.ProductRepository;
import com.netbull.apiloja.domain.store.Store;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.NotFoundException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class OrderService {

    private OrderRepository orderRepository;

    private StoreService storeService;

    private ProductRepository productRepository;

    private RabbitTemplate rabbitTemplate;

    public OrderService(OrderRepository orderRepository, StoreService storeService,
                        ProductRepository productRepository, RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.storeService = storeService;
        this.productRepository = productRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void persistOrder(Order order) {
        Store store = storeService.getStoreByID(order.getStore().getId());
        order.setStore(store);

        Client client = order.getClient();

        AddressClient addressClient = order.getAddress();
        addressClient.setClient(client);

        order.getProducts().forEach(productOrder -> productOrder.setOrder(order));

        for(ProductOrder productOrder : order.getProducts()) {
            Product product = productRepository
                    .findProductsByStoreAndCode(store, productOrder.getCode()).get();

            product.setQuantity(product.getQuantity().subtract(
                    productOrder.getQuantity()
            ));
            productRepository.save(product);
        }
        orderRepository.save(order);
    }

    @Transactional
    public void setOrderStateToDispatched(BigInteger id, String userEmail, OrderState orderState) {

        Order order = orderRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Nenhum pedido foi encontrado com o id: " + id + "."));

        if(order.getState().equals(OrderState.ENTREGUE)) {
            throw new IllegalArgumentException("O pedido já foi entregue na data: " +
                    order.getOrderDelivered().format(DateTimeFormatter.ofPattern("dd/MM/YYYY")));
        }

        if (!order.getStore().getEmail().equals(userEmail)) {
            throw new NotFoundException("Nenhum pedido foi encontrado com o id: " + id + ".");
        }

        if(!orderState.equals(OrderState.ENVIADO)) {
            throw new IllegalArgumentException("Somente é possível alterar o estado do pedido para: ENVIADO.");
        }

        order.setOrderDispatched(LocalDate.now());
        order.setState(orderState);

        if (orderRepository.save(order) != null) {
            log.info("Pedido alterado: {}", order.getState());
        }
        this.rabbitTemplate.convertAndSend("order-client", "order.client.updated.dispatched", order);
    }

    public Page<Order> getOrdersPageByStore(Pageable pageable, String userEmail) {
        Store store = storeService.getStoreByEmail(userEmail);

        Page<Order> ordersPage = orderRepository.findOrdersPageByStore(pageable, store);

        if (ordersPage.isEmpty()) {
            throw new NotFoundException("Nenhum pedido foi encontrado.");
        }

        return ordersPage;
    }
}
