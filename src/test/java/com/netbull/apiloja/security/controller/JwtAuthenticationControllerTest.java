package com.netbull.apiloja.security.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.netbull.apiloja.domain.store.Store;
import com.netbull.apiloja.security.model.JwtRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
class JwtAuthenticationControllerTest {

    private static final String URI_AUTH = "/authenticate";
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
                .apply(springSecurity())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();

        mapper.setAnnotationIntrospector(INTROSPECTOR);
        Store store = new Store();
        store.setCnpj("78965895687596");
        store.setEmail("crisdewes25@dewes");
        store.setPhone("74589658923");
        store.setPassword("123");
        store.setCorporateName("CRIS");

        ResultActions resultCreated = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_STORE)
                                .accept(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(store))
                ).andDo(print())
                .andExpect(status().isCreated());

        String messageResultCreated = resultCreated.andReturn().getResponse().getContentAsString();

        assertEquals("Loja criada.", messageResultCreated);
    }

    @Test
    @DisplayName("Testa autenticação sem dados para login")
    public void test_envioCamposSemDados_retona400() throws Exception {

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_AUTH)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(new JwtRequest()))
                ).andDo(print())
                .andExpect(status().isBadRequest());

        assertEquals("Credencial inválida.", resultActions.andReturn().getResponse().getContentAsString());

    }

    @Test
    @DisplayName("Testa autenticação com dados corretos")
    public void test_envioCamposComDadosIncorretos_retona400() throws Exception {

        JwtRequest jwtRequest = new JwtRequest("cris@dewes", "admin");

        ResultActions resultActionsPasswordIncorret = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_AUTH)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jwtRequest))
                ).andDo(print())
                .andExpect(status().isBadRequest());

        assertEquals("Credencial inválida.", resultActionsPasswordIncorret
                .andReturn()
                .getResponse()
                .getContentAsString());

        jwtRequest.setPassword("abc");
        jwtRequest.setUsername("email@");

        ResultActions resultActionsEmailIncorret = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_AUTH)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jwtRequest))
                ).andDo(print())
                .andExpect(status().isBadRequest());

        assertEquals("Credencial inválida.", resultActionsEmailIncorret
                .andReturn()
                .getResponse()
                .getContentAsString());

        jwtRequest.setPassword("abc");
        jwtRequest.setUsername("email@");

        ResultActions resultActionsEmailANDPasswordIncorret = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_AUTH)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jwtRequest))
                ).andDo(print())
                .andExpect(status().isBadRequest());

        assertEquals("Credencial inválida.", resultActionsEmailANDPasswordIncorret
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    @Test
    @DisplayName("Testa autenticação com dados para login, porém incorretos.")
    public void test_envioCamposComDadosCorretos_retona200() throws Exception {

        JwtRequest jwtRequest = new JwtRequest("crisdewes25@dewes", "123");

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_AUTH)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jwtRequest))
                ).andDo(print())
                .andExpect(status().isOk());

        HashMap<String, String> jwtResponse = mapper.readValue(resultActions
                .andReturn()
                .getResponse()
                .getContentAsString(), HashMap.class);

        assertTrue(jwtResponse.containsKey("jwtToken"));
    }
}