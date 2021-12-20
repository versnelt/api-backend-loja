package com.netbull.apiloja.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.netbull.apiloja.domain.order.Order;
import com.netbull.apiloja.domain.order.OrderRepository;
import com.netbull.apiloja.domain.order.OrderState;
import com.netbull.apiloja.domain.order.addressClient.AddressClient;
import com.netbull.apiloja.domain.order.addressClient.Type;
import com.netbull.apiloja.domain.order.addressClient.TypeRepository;
import com.netbull.apiloja.domain.order.client.Client;
import com.netbull.apiloja.domain.order.product.ProductOrder;
import com.netbull.apiloja.domain.product.Product;
import com.netbull.apiloja.domain.product.ProductRepository;
import com.netbull.apiloja.domain.store.Store;
import com.netbull.apiloja.domain.store.StoreRepository;
import com.netbull.apiloja.security.model.JwtRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
class OrderControllerTest {

    @Autowired
    OrderRepository orderRepository;

    private static final String URI_ORDER = "/v1/stores/orders";
    private static final String URI_STORE = "/v1/stores";
    private static final String URI_AUTH = "/authenticate";
    private AtomicInteger key = new AtomicInteger(1);
    private Store logger = new Store();


    private StringBuilder bearerToken = new StringBuilder();

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    private ProductOrder productOrder2 = new ProductOrder();
    private ProductOrder productOrder = new ProductOrder();

    private static final JacksonAnnotationIntrospector INTROSPECTOR = new JacksonAnnotationIntrospector() {
        @Override
        protected <A extends Annotation> A _findAnnotation(final Annotated annotated, final Class<A> annoClass) {
            if (!annotated.hasAnnotation(JsonProperty.class)) {
                return super._findAnnotation(annotated, annoClass);
            }
            return null;
        }
    };

    @BeforeAll
    public void setup() {

        this.mvc = MockMvcBuilders.webAppContextSetup(this.wac)
                .apply(springSecurity())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @BeforeEach()
    public void beforeEach() throws Exception {
        mapper.setAnnotationIntrospector(INTROSPECTOR);

        Store store = new Store();
        String cnpjKey = "";
        if(key.toString().toCharArray().length < 2) {
            cnpjKey = "5".concat(key.toString());
        } else {
            cnpjKey = key.toString();
        }

        store.setCnpj("758962354698".concat(cnpjKey));
        store.setEmail("crisdewes@dewes".concat(key.toString()));
        store.setPhone("111111111".concat(cnpjKey));
        store.setPassword("123");
        store.setCorporateName("CRIS");

        key.incrementAndGet();

        logger = store;

        ResultActions resultCreated = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_STORE)
                                .accept(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(store))
                ).andDo(print())
                .andExpect(status().isCreated());

        String messageResultCreated = resultCreated.andReturn().getResponse().getContentAsString();

        assertEquals("Loja criada.", messageResultCreated);

        JwtRequest jwtRequest = new JwtRequest(store.getEmail(), store.getPassword());

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_AUTH)
                                .accept(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jwtRequest))
                ).andDo(print())
                .andExpect(status().isOk());

        HashMap<String, String> jwtResponse = mapper.readValue(resultActions
                .andReturn()
                .getResponse()
                .getContentAsString(), HashMap.class);

        bearerToken = new StringBuilder();
        jwtResponse.values().stream().forEach(token -> bearerToken.append("Bearer " + token));
        assertTrue(jwtResponse.containsKey("jwtToken"));

        Order order = new Order();

        productOrder.setId(BigInteger.ONE);
        productOrder.setCode("1A8579");
        productOrder.setQuantity(BigInteger.ONE);
        productOrder.setPrice(BigDecimal.valueOf(20));
        productOrder.setOrder(order);


        productOrder2.setId(BigInteger.TWO);
        productOrder2.setCode("2");
        productOrder2.setQuantity(BigInteger.ONE);
        productOrder2.setPrice(BigDecimal.valueOf(10));
        productOrder2.setOrder(order);

        Client client = new Client();
        client.setId(BigInteger.ONE);

        Type type = new Type();
        type.setId(Integer.valueOf(1));
        type.setDescription("casa");

        AddressClient addressClient = new AddressClient();
        addressClient.setId(BigInteger.ONE);
        addressClient.setClient(client);
        addressClient.setState("rs");
        addressClient.setType(type);
        addressClient.setCep("95773000");
        addressClient.setCity("alto feliz");
        addressClient.setDistrict("centro");
        addressClient.setNumber("125");
        addressClient.setStreet("rua");


        order.setId(BigInteger.ONE);
        order.setProducts(List.of(productOrder2, productOrder));
        order.setStore(storeRepository.findByEmail(logger.getEmail()).get());
        order.setClient(client);
        order.setOrderCreated(LocalDate.now());
        order.setState(OrderState.CRIADO);
        order.setTotalValue(productOrder.getPrice().add(productOrder2.getPrice()));
        order.setAddress(addressClient);

        orderRepository.save(order);
    }

    @Test
    @DisplayName("Testa busca dos pedidos da loja logada.")
    public void test_buscaPedidoDaLojaLogada_retorna200() throws Exception {

        ResultActions resultActions = mvc.perform(
                    MockMvcRequestBuilders.get(URI_ORDER)
                            .header("Authorization", bearerToken.toString())
                            .accept(MediaType.APPLICATION_JSON)
            ).andExpect(status().isOk());

        assertTrue(resultActions.andReturn().getResponse().getContentAsString().contains(
                productOrder.getCode()
        ));
        assertTrue(resultActions.andReturn().getResponse().getContentAsString().contains(
                productOrder2.getCode()
        ));
    }

    @Test
    @DisplayName("Testa busca dos pedidos da loja logada quando não encontra pedido.")
    @WithMockUser("acr@d")
    public void test_buscaPedidoDaLojaLogadaQuandoNaoEncontraPedido_retorna404() throws Exception {

        Store store = new Store();
        store.setCnpj("75896235469998");
        store.setEmail("acr@d");
        store.setPhone("11188888888");
        store.setPassword("123");
        store.setCorporateName("CRIS");
        storeRepository.save(store);

        ResultActions resultActions = mvc.perform(
                MockMvcRequestBuilders.get(URI_ORDER)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound());

        assertEquals("Nenhum pedido foi encontrado.",
                resultActions.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Testa alteração do estado do pedido quando envia valor diferente de ENVIADO.")
    public void test_alteracaoDoEstadoDoPedidoQuandoEnviaValorDiferenteDeEntregue_retorna400() throws Exception {

        Order order1 = new Order();
        order1.setState(OrderState.CRIADO);

        ResultActions resultpatch = mvc.perform(
                        MockMvcRequestBuilders.patch(URI_ORDER.concat("/{id}"), 1)
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(order1))
                ).andDo(print())
                .andExpect(status().isBadRequest());

        assertEquals("Somente é possível alterar o estado do pedido para: ENVIADO.",
                resultpatch.andReturn().getResponse().getContentAsString());

        order1.setState(OrderState.ENTREGUE);

        ResultActions resultpatch2 = mvc.perform(
                        MockMvcRequestBuilders.patch(URI_ORDER.concat("/{id}"), 1)
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(order1))
                ).andDo(print())
                .andExpect(status().isBadRequest());

        assertEquals("Somente é possível alterar o estado do pedido para: ENVIADO.",
                resultpatch2.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Testa alteração do estado do pedido para ENTREGUE.")
    public void test_alteracaoDoEstadoDoPedidoParaEntregue_retorna200() throws Exception {

        Order orderEntregue = new Order();
        orderEntregue.setState(OrderState.ENVIADO);

        ResultActions resultpatch = mvc.perform(
                        MockMvcRequestBuilders.patch(URI_ORDER.concat("/{id}"), 1)
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(orderEntregue))
                ).andDo(print())
                .andExpect(status().isOk());

        assertEquals("Pedido alterado para enviado.",
                resultpatch.andReturn().getResponse().getContentAsString());
    }
}