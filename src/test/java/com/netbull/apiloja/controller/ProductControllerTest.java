package com.netbull.apiloja.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.netbull.apiloja.domain.product.Product;
import com.netbull.apiloja.domain.store.Store;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.method.P;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
class ProductControllerTest {

    private static final String URI_PRODUCT = "/v1/stores/products";
    private static final String URI_STORE = "/v1/stores";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

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
    public void setup() throws Exception {
        this.mvc = MockMvcBuilders.webAppContextSetup(this.wac)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();

        mapper.setAnnotationIntrospector(INTROSPECTOR);

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("11115511111111");
        store.setEmail("a@crisD");
        store.setPhone("11155511111");
        store.setPassword("abc");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_STORE)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(store))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Loja criada.", resultActions.andReturn().getResponse().getContentAsString());

    }

    @BeforeEach
    public void beforeEach() {
        mapper.setAnnotationIntrospector(INTROSPECTOR);
    }

    @Test
    @Order(1)
    @DisplayName("Testa persistir produto sem campos obrigatórios.")
    @WithMockUser("a@crisD")
    public void test_persistirProdutoSemCamposObrigatorios_retorna400() throws Exception {

        this.mvc.perform(
                MockMvcRequestBuilders.post(URI_PRODUCT)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new Product()))
        ).andExpect(status().isBadRequest());
    }

    @Test
    @Order(2)
    @DisplayName("Testa persistir produto com campos obrigatórios.")
    @WithMockUser("a@crisD")
    public void test_persistirProdutoComCamposObrigatorios_retorna201() throws Exception {

        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51dasgdaf");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_PRODUCT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(product))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Produto criado.", resultActions.andReturn().getResponse().getContentAsString());
    }

    @Test
    @Order(3)
    @DisplayName("Testa busca produto pelo ID.")
    @WithMockUser("a@crisD")
    public void test_getProdutoPeloId_retorna200() throws Exception {

        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500.00));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51asdasfd");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_PRODUCT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(product))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Produto criado.", resultActions.andReturn().getResponse().getContentAsString());

        ResultActions resultGet = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultActions.andReturn().getResponse().getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        Product productResultGet = this.mapper.readValue(resultGet.andReturn().getResponse().getContentAsString(),
                Product.class);

        assertEquals(product.getDescription(), productResultGet.getDescription());
        assertEquals(product.getCode(), productResultGet.getCode());
    }

    @Test
    @Order(4)
    @DisplayName("Testa busca produto quando ID não existe.")
    @WithMockUser("a@crisD")
    public void test_getProdutoQuandoNaoExiste_retorna404() throws Exception {

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_PRODUCT.concat("/158956"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Nenhum produto foi encontrado.",
                resultActions.andReturn().getResponse().getContentAsString());
    }

    @Test
    @Order(5)
    @DisplayName("Testa busca de todos produtos da loja quando não existe produto cadastrado.")
    @WithMockUser("a@crisDewes")
    public void test_getProdutosDaLojaQuandoNaoExiste_retorna404() throws Exception {

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("11115591111111");
        store.setEmail("a@crisDewes");
        store.setPhone("11155591111");
        store.setPassword("abc");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_STORE)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(store))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Loja criada.", resultActions.andReturn().getResponse().getContentAsString());


        ResultActions resultActionsGet = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_PRODUCT)
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Nenhum produto foi encontrado.",
                resultActionsGet.andReturn().getResponse().getContentAsString());
    }

    @Test
    @Order(6)
    @DisplayName("Testa busca de todos produtos cadastrados pela loja.")
    @WithMockUser("a@crisD")
    public void test_getProdutosDaLoja_retorna200() throws Exception {

        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500.00));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51sfgnfghn");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_PRODUCT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(product))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Produto criado.", resultActions.andReturn().getResponse().getContentAsString());

        this.mvc.perform(MockMvcRequestBuilders.get(URI_PRODUCT)
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    @DisplayName("Testa alteração do preço do produto.")
    @WithMockUser("a@crisD")
    public void test_patchPrecoDoProduto_retorna201() throws Exception {

        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500.00));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51hfmfghjmf");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_PRODUCT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(product))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Produto criado.", resultActions.andReturn().getResponse().getContentAsString());

        BigDecimal price = new BigDecimal(365.000);

        ResultActions resultActionsPatch = this.mvc.perform(
                MockMvcRequestBuilders.patch(resultActions.andReturn().getResponse().getHeader("Location")
                        .concat("/price/{price}"),price))
                .andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Preço alterado.", resultActionsPatch.andReturn().getResponse().getContentAsString());

        ResultActions resultGet = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultActionsPatch.andReturn()
                                        .getResponse().getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        Product productResultGet = this.mapper.readValue(resultGet.andReturn().getResponse().getContentAsString(),
                Product.class);

        assertEquals(0, price.compareTo(productResultGet.getPrice()));
        assertEquals(product.getCode(), productResultGet.getCode());
    }

    @Test
    @Order(8)
    @DisplayName("Testa a alteração de preço quando envia valor negativo.")
    @WithMockUser("a@crisD")
    public void test_patchPrecoProdutoQuandoEnviaPrecoMenorDoQueZero_retorna400() throws Exception {
        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500.00));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51sdfsbngf");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_PRODUCT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(product))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Produto criado.", resultActions.andReturn().getResponse().getContentAsString());

        BigDecimal price = new BigDecimal(-0.5);

        ResultActions resultActionsPatch = this.mvc.perform(
                        MockMvcRequestBuilders.patch(resultActions.andReturn().getResponse().getHeader("Location")
                                .concat("/price/{price}"),price))
                .andDo(print())
                .andExpect(status().isBadRequest());

        assertEquals("O preço não pode ser negativo.", resultActionsPatch.andReturn().getResponse().getContentAsString());
    }

    @Test
    @Order(9)
    @DisplayName("Testa a alteração de preço quando envia id inválido.")
    @WithMockUser("a@crisD")
    public void test_patchPrecoProdutoQuandoEnviaIdInvalido_retorna404() throws Exception {
        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500.00));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51adsf");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_PRODUCT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(product))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Produto criado.", resultActions.andReturn().getResponse().getContentAsString());

        BigDecimal price = new BigDecimal(0.5);

        ResultActions resultActionsPatch = this.mvc.perform(
                        MockMvcRequestBuilders.patch(URI_PRODUCT.concat("/130/price/{price}"), price))
                .andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("O produto não foi encontrado.", resultActionsPatch.andReturn().getResponse().getContentAsString());
    }

    @Test
    @Order(10)
    @DisplayName("Testa alteração dos dados do produto quando estão dentro do padrão.")
    @WithMockUser("a@crisD")
    public void test_alteracaoDadosProdutos_retorna201() throws Exception{
        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500.00));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51as");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_PRODUCT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(product))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Produto criado.", resultActions.andReturn().getResponse().getContentAsString());

        Product newProduct = new Product();
        newProduct.setName("TV");
        newProduct.setDescription("58");
        newProduct.setPrice(BigDecimal.valueOf(159350.00));
        newProduct.setQuantity(BigInteger.valueOf(20234));
        newProduct.setCode("8764930287256724");

        ResultActions resultActionsPut = this.mvc.perform(
                        MockMvcRequestBuilders.put(resultActions.andReturn().getResponse().getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(newProduct))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Produto alterado.", resultActionsPut.andReturn().getResponse().getContentAsString());

        ResultActions resultActionsGet = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultActions.andReturn().getResponse().getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        Product productGetResult = mapper.readValue(resultActionsGet
                .andReturn()
                .getResponse()
                .getContentAsString(),
                Product.class);

        assertEquals(newProduct.getCode(), productGetResult.getCode());
    }

    @Test
    @Order(11)
    @DisplayName("Testa alteração dos dados do produto quando não encontra o produto.")
    @WithMockUser("a@crisD")
    public void test_alteracaoDadosProdutosQuandoNaoEncontra_retorna404() throws Exception{

        ResultActions resultActionsPut = this.mvc.perform(
                        MockMvcRequestBuilders.put(URI_PRODUCT.concat("/56"))
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(new Product())))
                .andExpect(status().isNotFound());

        assertEquals("O produto não foi encontrado.",
                resultActionsPut.andReturn().getResponse().getContentAsString());
    }

    @Test
    @Order(12)
    @DisplayName("Testa alteração dos dados do produto quando estão fora do padrão.")
    @WithMockUser("a@crisD")
    public void test_alteracaoDadosProdutosQuandoEnviaForaDoPadrao_retorna400() throws Exception{
        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500.00));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_PRODUCT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(product))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Produto criado.", resultActions.andReturn().getResponse().getContentAsString());

        StringBuilder description = new StringBuilder();
        for(int x = 0; x<1000; x++) {
            description.append("a");
        }

        StringBuilder name = new StringBuilder();
        for(int x = 0; x<1000; x++) {
            name.append("a");
        }

        Product newProduct = new Product();
        newProduct.setName(name.toString());
        newProduct.setDescription(description.toString());
        newProduct.setPrice(BigDecimal.valueOf(-159350.00));
        newProduct.setQuantity(BigInteger.valueOf(-1234524));
        newProduct.setCode("8764930287256724");

        ResultActions resultActionsPut = this.mvc.perform(
                        MockMvcRequestBuilders.put(resultActions.andReturn().getResponse().getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(newProduct))
                ).andDo(print())
                .andExpect(status().isBadRequest());

        List<String> messages = mapper.readValue(resultActionsPut.andReturn().getResponse().getContentAsString(),
                List.class);

        List<String> expectMessages = List.of(Product.class.getName().concat(" name: O nome do produto é muito grande."),
                Product.class.getName().concat(" description: A descrição do produto é muito grande."),
                Product.class.getName().concat(" price: O preço não pode ser menor que R$0,00."),
                Product.class.getName().concat(" quantity: A quantidade não pode ser menor que 0."));

        messages.forEach(message -> assertTrue(expectMessages.contains(message)));
    }

    @Test
    @Order(13)
    @DisplayName("Testa delete de produto pelo ID.")
    @WithMockUser("a@crisD")
    public void test_deleteDeProdutoPeloIdQuandoExiste_retorna200() throws Exception{
        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500.00));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51dafbdfb");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_PRODUCT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(product))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Produto criado.", resultActions.andReturn().getResponse().getContentAsString());

        ResultActions resultActionsGet = this.mvc.perform(
                        MockMvcRequestBuilders.delete(resultActions.andReturn().getResponse().getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        assertEquals("Produto deletado.", resultActionsGet.andReturn().getResponse().getContentAsString());
    }

    @Test
    @Order(14)
    @DisplayName("Testa delete de produto quando não encontra.")
    @WithMockUser("a@crisD")
    public void test_deleteDeProdutoQuandoNaoEncontra_retorna404() throws Exception{

        ResultActions resultActionsPut = this.mvc.perform(
                        MockMvcRequestBuilders.delete(URI_PRODUCT.concat("/56"))
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(new Product())))
                .andExpect(status().isNotFound());

        assertEquals("O produto não foi encontrado.",
                resultActionsPut.andReturn().getResponse().getContentAsString());
    }
}