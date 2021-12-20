package com.netbull.apiloja.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
class StoreControllerTest {
    private static final String URI_STORE = "/v1/stores";

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
    @DisplayName("Testa persistir loja sem campos obrigatórios.")
    public void test_persistirLojaSemCamposObrigatorios_retorna400() throws Exception {

        this.mvc.perform(
                MockMvcRequestBuilders.post(URI_STORE)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new Store()))
        ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Testa persistir loja com campos obrigatórios.")
    public void test_persistirLojaComCampoObrigatorios_retorna201() throws Exception {
        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("11111111111111");
        store.setEmail("a@com1");
        store.setPhone("11111111111");
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

    @Test
    @DisplayName("Testa busca de loja por id, quando encontra.")
    public void test_buscaLojaPorId_retorna200() throws Exception {
        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("11111111111112");
        store.setEmail("a@com2");
        store.setPhone("11111111112");
        store.setPassword("abc");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_STORE)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(store))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Loja criada.", resultActions.andReturn().getResponse().getContentAsString());

        ResultActions resultActionsBusca = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultActions.andReturn().getResponse().getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        Store storeResult = mapper.readValue(resultActionsBusca
                .andReturn()
                .getResponse()
                .getContentAsString(), Store.class);

        assertEquals(store.getCnpj(), storeResult.getCnpj());
        assertEquals(store.getEmail(), storeResult.getEmail());
    }

    @Test
    @DisplayName("Testa busca de loja quando id não existe.")
    public void test_buscaLojaPorIdQuandoNaoExiste_retorna400() throws Exception {

        ResultActions resultActionsBusca = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_STORE.concat("/100000"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Nenhuma loja foi encontrada.",
                resultActionsBusca.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Testa busca de todas lojas.")
    public void test_buscaTodasLojas_retorna200() throws Exception{
        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("11111111111113");
        store.setEmail("a@com3");
        store.setPhone("11111111113");
        store.setPassword("abc");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_STORE)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(store))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Loja criada.", resultActions.andReturn().getResponse().getContentAsString());

        this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_STORE)
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Testa busca de loja por e-mail, quando encontra.")
    public void test_buscaLojaPorCNPJ_retorna200() throws Exception {
        Store store = new Store();
        store.setCorporateName("Versnelt4");
        store.setCnpj("11111111111114");
        store.setEmail("a@com4");
        store.setPhone("11111111114");
        store.setPassword("abc");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_STORE)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(store))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Loja criada.", resultActions.andReturn().getResponse().getContentAsString());

        ResultActions resultActionsBusca = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_STORE.concat("/email/{email}"), store.getEmail())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        Store storeResult = mapper.readValue(resultActionsBusca
                .andReturn()
                .getResponse()
                .getContentAsString(), Store.class);

        assertEquals(store.getCnpj(), storeResult.getCnpj());
        assertEquals(store.getEmail(), storeResult.getEmail());
    }

    @Test
    @DisplayName("Testa busca de loja quando email não existe.")
    public void test_buscaLojaPorEmailQuandoNaoExiste_retorna400() throws Exception {

        ResultActions resultActionsBusca = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_STORE.concat("/email/cris@testes"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Nenhuma loja foi encontrada.",
                resultActionsBusca.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Testa alteração da Loja quando ela existe.")
    @WithMockUser("a@com5")
    public void test_alterLojaQuandoExiste_retorna201() throws Exception {
        Store store = new Store();
        store.setCorporateName("Versnelt5");
        store.setCnpj("11111111111115");
        store.setEmail("a@com5");
        store.setPhone("11111111115");
        store.setPassword("abc");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_STORE)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(store))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Loja criada.", resultActions.andReturn().getResponse().getContentAsString());

        Store newStore = new Store();
        newStore.setCorporateName("Cristiano");
        newStore.setCnpj("11111111111125");
        newStore.setEmail("a@atos.net");
        newStore.setPhone("11111111815");
        newStore.setPassword("abc");

        ResultActions resultActionsPut = this.mvc.perform(
                        MockMvcRequestBuilders.put(URI_STORE)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(newStore))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Loja alterada.", resultActionsPut.andReturn().getResponse().getContentAsString());

        ResultActions resultActionsBusca = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultActionsPut.andReturn().getResponse().getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        Store storeResult = mapper.readValue(resultActionsBusca
                .andReturn()
                .getResponse()
                .getContentAsString(), Store.class);

        assertEquals(newStore.getCnpj(), storeResult.getCnpj());
        assertEquals(newStore.getEmail(), storeResult.getEmail());
    }

    @Test
    @DisplayName("Testa delete de Loja.")
    @WithMockUser("a@com7")
    public void test_deleteDeLoja_retorna200() throws Exception {

        Store store = new Store();
        store.setCorporateName("Versnelt7");
        store.setCnpj("11111111111117");
        store.setEmail("a@com7");
        store.setPhone("11111111117");
        store.setPassword("abc");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_STORE)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(store))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Loja criada.", resultActions.andReturn().getResponse().getContentAsString());

        ResultActions resultActionsDelete = this.mvc.perform(
                MockMvcRequestBuilders.delete(URI_STORE)
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print())
                .andExpect(status().isOk());

        assertEquals("Loja deletada.", resultActionsDelete.andReturn().getResponse().getContentAsString());
    }
}