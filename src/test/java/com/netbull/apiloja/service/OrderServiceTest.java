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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import javax.ws.rs.NotFoundException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderServiceTest {

    OrderRepository orderRepository;

    StoreService storeService;

    ProductRepository productRepository;

    RabbitTemplate rabbitTemplate;

    OrderService orderService;

    Pageable pageable;

    @BeforeAll
    public void setupBeforAll() {
    }

    @BeforeEach
    public void setupBeforeEach() {
        this.orderRepository = Mockito.mock(OrderRepository.class);
        this.storeService = Mockito.mock(StoreService.class);
        this.productRepository = Mockito.mock(ProductRepository.class);
        this.rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        this.pageable = Mockito.mock(Pageable.class);
        this.orderService = new OrderService(orderRepository, storeService,
                productRepository, rabbitTemplate);
    }

    @Test
    @DisplayName("Testa persistir pedido, verificando se o pedido é salvo e se a " +
            "quantidade dos produtos são alteradas.")
    public void test_persistOrder() {
        assertNotNull(orderService);

        Product product = new Product();
        product.setQuantity(BigInteger.valueOf(500));
        product.setCode("123");

        ProductOrder productOrder = new ProductOrder();
        productOrder.setQuantity(BigInteger.valueOf(398));
        productOrder.setCode("123");

        AddressClient addressClient = new AddressClient();

        Client client = new Client();

        Store store = new Store();
        store.setId(BigInteger.ONE);

        Order order = new Order();
        order.setProducts(List.of(productOrder));
        order.setAddress(addressClient);
        order.setStore(store);
        order.setClient(client);

        when(storeService.getStoreByID(any())).thenReturn(store);
        when(productRepository.findProductsByStoreAndCode(eq(store), eq("123")))
                .thenReturn(Optional.of(product));

        orderService.persistOrder(order);

        assertEquals(BigInteger.valueOf(500).subtract(productOrder.getQuantity())
                , product.getQuantity());
        assertEquals(client, addressClient.getClient());
        assertEquals(order, productOrder.getOrder());
        then(orderRepository).should(times(1)).save(order);
    }

    @Test
    @DisplayName("Testa alteração do pedido para entregue quando não encontra.")
    public void test_alteracaoDoPedidoParaEnviadoQuandoNaoEncontra_lancaException() {
        assertNotNull(orderService);

        when(orderRepository.findById(any())).thenReturn(Optional.empty());
        var assertThrows = assertThrows(NotFoundException.class,
                () -> orderService.setOrderStateToDispatched(BigInteger.ONE, "", OrderState.ENVIADO));

        assertEquals("Nenhum pedido foi encontrado com o id: " + BigInteger.ONE + ".",
                assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do pedido para entregue quando o pedido já está como entregue.")
    public void test_alteracaoDoPedidoParaEnviadoQuandoPedidJaEntregue_lancaException() {
        assertNotNull(orderService);

        Store store = new Store();
        store.setEmail("a@a");

        Order order = new Order();
        order.setStore(store);
        order.setState(OrderState.ENTREGUE);
        order.setOrderDelivered(LocalDate.now());

        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> orderService.setOrderStateToDispatched(BigInteger.ONE, "a@a", OrderState.ENVIADO));

        assertEquals("O pedido já foi entregue na data: " +
                        order.getOrderDelivered().format(DateTimeFormatter.ofPattern("dd/MM/YYYY")),
                assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do pedido para entregue quando não pertence à loja logada.")
    public void test_alteracaoDoPedidoParaEntregueQuandoNaoPertenceALojaLogada_lancaException() {
        assertNotNull(orderService);

        Store store = new Store();
        store.setEmail("a@a");

        Order order = new Order();
        order.setStore(store);
        order.setState(OrderState.CRIADO);

        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        var assertThrows = assertThrows(NotFoundException.class,
                () -> orderService.setOrderStateToDispatched(BigInteger.ONE, "t", OrderState.ENTREGUE));

        assertEquals("Nenhum pedido foi encontrado com o id: " + BigInteger.ONE + ".",
                assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do pedido para outro estado que não seja para ENVIADO.")
    public void test_alteracaoDoPedidoParaOutroEstadoQueNaoSejaParaEnviado_lancaException() {
        assertNotNull(orderService);

        Store store = new Store();
        store.setEmail("a@A");

        Order order = new Order();
        order.setStore(store);
        order.setState(OrderState.CRIADO);

        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> orderService.setOrderStateToDispatched(BigInteger.ONE, "a@A", OrderState.CRIADO));

        assertEquals("Somente é possível alterar o estado do pedido para: ENVIADO.",
                assertThrows.getMessage());

        var assertThrows2 = assertThrows(IllegalArgumentException.class,
                () -> orderService.setOrderStateToDispatched(BigInteger.ONE, "a@A", OrderState.ENTREGUE));

        assertEquals("Somente é possível alterar o estado do pedido para: ENVIADO.",
                assertThrows2.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do pedido para enviado quando dados corretos.")
    public void test_alteracaoDoPedidoParaEnviadoQuandoDadosCorretos() {
        assertNotNull(orderService);

        Store store = new Store();
        store.setEmail("a@A");

        Order order = new Order();
        order.setStore(store);
        order.setState(OrderState.CRIADO);

        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        orderService.setOrderStateToDispatched(BigInteger.ONE, store.getEmail(), OrderState.ENVIADO);

        assertEquals(order.getState(), OrderState.ENVIADO);
        assertEquals(order.getOrderDispatched(), LocalDate.now());
        then(orderRepository).should(times(1)).save(any());
        then(rabbitTemplate).should(times(1)).convertAndSend("order-client",
                "order.client.updated.dispatched", order);
    }

    @Test
    @DisplayName("Testa busca de todos pedidos da loja logada quando não encontra nenhum.")
    public void test_buscaTodosPedidoDoClientQuandoNaoEncontra_lancaEception(){
        assertNotNull(orderService);

        Page<Order> ordersPage = new PageImpl<>(new ArrayList<>(), this.pageable, 0);
        when(orderRepository.findOrdersPageByStore(any(), any())).thenReturn(ordersPage);

        var assertThrows = assertThrows(NotFoundException.class,
                () -> orderService.getOrdersPageByStore(this.pageable, ""));

        assertEquals("Nenhum pedido foi encontrado.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa busca de todos pedidos da loja logada.")
    public void test_buscaTodosPedidoDoClient(){
        assertNotNull(orderService);

        List<Order> ordersArray = new ArrayList<>();
        for(int x = 0; x < 100; x++) {
            ordersArray.add(new Order());
        }

        Page<Order> ordersPage = new PageImpl<>(ordersArray, this.pageable, 0);
        when(orderRepository.findOrdersPageByStore(any(), any())).thenReturn(ordersPage);


        List<Order> ordersPageGet = orderService.getOrdersPageByStore(this.pageable, "").toList();

        assertNotNull(ordersPageGet);
        assertEquals(ordersArray.size(), ordersPageGet.size());
    }
}