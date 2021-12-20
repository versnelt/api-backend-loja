package com.netbull.apiloja.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.netbull.apiloja.domain.address.addressStore.AddressStore;
import com.netbull.apiloja.domain.store.Store;
import com.netbull.apiloja.domain.store.StoreRepository;
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
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
public class AddressControllerTest {

    private static final String URI_ADDRESS = "/v1/stores/addresses";

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    @BeforeAll
    public void setup() {
        this.mvc = MockMvcBuilders.webAppContextSetup(this.wac)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    private static final JacksonAnnotationIntrospector INTROSPECTOR = new JacksonAnnotationIntrospector() {
        @Override
        protected <A extends Annotation> A _findAnnotation(final Annotated annotated, final Class<A> annoClass) {
            if (!annotated.hasAnnotation(JsonProperty.class)) {
                return super._findAnnotation(annotated, annoClass);
            }
            return null;
        }
    };

    @BeforeEach
    public void beforeEach() {
        mapper.setAnnotationIntrospector(INTROSPECTOR);
    }

    @Test
    @DisplayName("Envio de endereco sem campos obrigatórios")
    @WithMockUser("2a@com1")
    public void test_envioCamposSemDados_retona400() throws Exception {
        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("21111111111111");
        store.setEmail("2a@com1");
        store.setPhone("21111111111");
        store.setPassword("abc");

        storeRepository.save(store);

        AddressStore address = new AddressStore();

        this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(address))
                ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cria endereço")
    @WithMockUser("3a@com2")
    public void test_criarEndereco_retona201() throws Exception {

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("31111111111112");
        store.setEmail("3a@com2");
        store.setPhone("31111111111");
        store.setPassword("abc");

        storeRepository.save(store);

        AddressStore address = new AddressStore();
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");

        this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Busca endereço por id, quando existe e quando não existe")
    @WithMockUser("4a@com3")
    public void test_buscaEnderecoPorId() throws Exception {

        ResultActions resultGetNull = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS.concat("/{id}"), "1")
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Endereço não encontrado",
                resultGetNull.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("41111111111113");
        store.setEmail("4a@com3");
        store.setPhone("41111111111");
        store.setPassword("abc");

        storeRepository.save(store);

        AddressStore address = new AddressStore();
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");

        ResultActions resultCreatedAddress = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isCreated());

        ResultActions resultGetAddress = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultCreatedAddress
                                        .andReturn()
                                        .getResponse().getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        AddressStore addressResponse = mapper.readValue(resultGetAddress.andReturn()
                        .getResponse()
                        .getContentAsString(),
                AddressStore.class);

        assertEquals(address.getStreet(), addressResponse.getStreet());
    }

    @Test
    @DisplayName("Busca endereço por store, quando existe e quando não existe")
    @WithMockUser("5a@com4")
    public void test_buscaEnderecoPorStore_retona200() throws Exception {

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("51111111111114");
        store.setEmail("5a@com4");
        store.setPhone("51111111111");
        store.setPassword("abc");

        storeRepository.save(store);

        ResultActions resultGetNull = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Endereço não encontrado",
                resultGetNull.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        AddressStore address = new AddressStore();
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");

        ResultActions resultCreatedAddress = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Endereço criado.", resultCreatedAddress.andReturn().getResponse().getContentAsString());

        ResultActions resultGetAddress = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        assertNotNull(resultGetAddress);
    }

    @Test
    @DisplayName("Altera o endereço.")
    @WithMockUser("6a@com6")
    public void test_alteraOEndereço() throws Exception {

        ResultActions resultGetNull = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS.concat("/{id}"), "100")
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Endereço não encontrado",
                resultGetNull.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("61111111111116");
        store.setEmail("6a@com6");
        store.setPhone("61111111111");
        store.setPassword("abc");

        storeRepository.save(store);

        AddressStore address = new AddressStore();
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");

        ResultActions resultCreatedAddress = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Endereço criado.", resultCreatedAddress.andReturn().getResponse().getContentAsString());

        address.setNumber("789");
        address.setDistrict("Bairro");
        address.setCity("Feliz");

        ResultActions resultPutAddress = this.mvc.perform(
                        MockMvcRequestBuilders.put(resultCreatedAddress
                                        .andReturn()
                                        .getResponse()
                                        .getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isOk());

        assertEquals("Endereço Alterado", resultPutAddress.andReturn().getResponse().getContentAsString());

        ResultActions resultGetAddress = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultCreatedAddress
                                        .andReturn()
                                        .getResponse()
                                        .getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        AddressStore addressReturn = mapper.readValue(resultGetAddress
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                AddressStore.class);

        assertEquals(address.getNumber(), addressReturn.getNumber());
        assertEquals(address.getDistrict(), addressReturn.getDistrict());
        assertEquals(address.getCity(), addressReturn.getCity());
    }

    @Test
    @DisplayName("Exclui um endereço.")
    @WithMockUser("7a@com7")
    public void test_deletaOEndereço() throws Exception {

        ResultActions resultGetNull = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS.concat("/{id}"), "100")
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Endereço não encontrado",
                resultGetNull.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("71111111111117");
        store.setEmail("7a@com7");
        store.setPhone("71111111111");
        store.setPassword("abc");

        storeRepository.save(store);

        AddressStore address = new AddressStore();
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");

        ResultActions resultCreatedAddress = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Endereço criado.", resultCreatedAddress.andReturn().getResponse().getContentAsString());

        ResultActions resultDeleteAddress = this.mvc.perform(
                        MockMvcRequestBuilders.delete(resultCreatedAddress
                                        .andReturn()
                                        .getResponse()
                                        .getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        assertEquals("Endereço deletado", resultDeleteAddress.andReturn().getResponse().getContentAsString());

        ResultActions resultGetExcluido = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultCreatedAddress
                                        .andReturn()
                                        .getResponse()
                                        .getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Endereço não encontrado",
                resultGetExcluido.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}