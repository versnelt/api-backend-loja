package com.netbull.apiloja.service;

import com.netbull.apiloja.domain.address.addressStore.AddressStoreRepository;
import com.netbull.apiloja.domain.address.addressStore.AddressStore;
import com.netbull.apiloja.domain.product.Product;
import com.netbull.apiloja.domain.product.ProductRepository;
import com.netbull.apiloja.domain.store.Store;
import com.netbull.apiloja.domain.store.StoreRepository;
import com.netbull.apiloja.utility.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import javax.validation.*;
import javax.ws.rs.NotFoundException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoreServiceTest {

    private StoreService storeService;

    private StoreRepository storeRepository;

    private AddressStoreRepository addressStoreRepository;

    private ProductRepository productRepository;

    private StringUtils stringUtils;

    private Validator validator;

    private RabbitTemplate rabbitTemplate;

    private Pageable pageable;

    @BeforeAll
    public void setupBeforAll() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        this.validator = validatorFactory.getValidator();
    }

    @BeforeEach
    public void setupBeforeEach() {
        this.storeRepository = Mockito.mock(StoreRepository.class);
        this.addressStoreRepository = Mockito.mock(AddressStoreRepository.class);
        this.productRepository = Mockito.mock(ProductRepository.class);
        this.pageable = Mockito.mock(Pageable.class);
        this.stringUtils = Mockito.mock(StringUtils.class);
        this.rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        storeService = new StoreService(validator, storeRepository,
                productRepository, addressStoreRepository,
                stringUtils, rabbitTemplate);
    }

    @Test
    @DisplayName("Testa persistir quando loja for nula.")
    public void test_quandoStoreNull_lancaException() {
        assertNotNull(storeService);

        Store store = null;

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> storeService.persistStore(store));
    }

    @Test
    @DisplayName("Testa persistir quando os atributos da loja são nulos.")
    public void test_quandoAtributosDaLojaNull_lancaException() {
        assertNotNull(storeService);

        Store store = new Store();

        var assertThrows = assertThrows(ConstraintViolationException.class,
                () -> storeService.persistStore(store));

        List<String> messages = assertThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertEquals(5, assertThrows.getConstraintViolations().size());
        assertThat(messages, hasItems("A razão social não pode ser vazia."
                , "O CNPJ não pode ser vazio."
                , "O e-mail não pode ser vazio."
                , "A senha não pode ser vazia."
                , "O número de telefone não pode ser vazio."));
    }

    @Test
    @DisplayName("Testa persistir quando os atributos estão fora do padrão.")
    public void test_quandoAtributosEstaoForaDoPadrao_lancaException() {
        assertNotNull(storeService);

        Store store = new Store();
        store.setCnpj("abc");
        store.setEmail("abc");
        store.setPhone("abc");
        store.setPassword("a");

        StringBuilder companyName = new StringBuilder();
        for (int x = 0; x < 100; x++) {
            companyName.append("a");
        }
        store.setCorporateName(companyName.toString());

        var asserThrows = assertThrows(ConstraintViolationException.class,
                () -> storeService.persistStore(store));

        assertNotNull(asserThrows);
        assertEquals(5, asserThrows.getConstraintViolations().size());

        List<String> messages = asserThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems("CNPJ inválido.",
                "A razão social é muito grande.",
                "E-mail inválido.",
                "Número de telefone inválido.",
                "A senha é muito pequena."));
    }

    @Test
    @DisplayName("Testa persist quando os atributos estão dentro do padrão")
    public void test_quandoAtributosEstaoNoPadrao() {
        assertNotNull(storeService);

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("11111111111111");
        store.setEmail("abc@abc");
        store.setPhone("11111111111");
        store.setPassword("abc");

        storeService.persistStore(store);
        then(storeRepository).should(times(1)).save(any());
        then(stringUtils).should(times(1)).encryptPassword(any());
    }

    @Test
    @DisplayName("Testa persist quando o CNPJ já está cadastrado")
    public void test_quandoCNPJaEstaCadastrado_lancaException() {
        assertNotNull(storeService);

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("11111111111111");
        store.setEmail("abc@abc");
        store.setPhone("11111111111");
        store.setPassword("abc");

        storeService.persistStore(store);
        then(storeRepository).should(times(1)).save(any());

        Store store2 = new Store();
        store2.setCorporateName("Versnelt");
        store2.setCnpj("11111111111111");
        store2.setEmail("abc@123");
        store2.setPassword("abc");
        store2.setPhone("11111111112");

        when(storeRepository.findByCnpj(store.getCnpj())).thenReturn(Optional.of(store));
        var assertThrows = assertThrows(DuplicateKeyException.class,
                () -> storeService.persistStore(store2));

        assertEquals("O CNPJ já está sendo utilizado", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa persist quando o e-mail já está cadastrado")
    public void test_quandoEmailaEstaCadastrado_lancaException() {
        assertNotNull(storeService);

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("11111111111111");
        store.setEmail("abc@abc");
        store.setPhone("11111111111");
        store.setPassword("abc");

        storeService.persistStore(store);
        then(storeRepository).should(times(1)).save(any());

        Store store2 = new Store();
        store2.setCorporateName("Versnelt");
        store2.setCnpj("11111111111112");
        store2.setEmail("abc@abc");
        store2.setPhone("11111111112");
        store2.setPassword("abc");

        when(storeRepository.findByEmail(store.getEmail())).thenReturn(Optional.of(store));
        var assertThrows = assertThrows(DuplicateKeyException.class,
                () -> storeService.persistStore(store2));

        assertEquals("O e-mail já está sendo utilizado", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa persist quando o telefone já está cadastrado")
    public void test_quandoTelefoneaEstaCadastrado_lancaException() {
        assertNotNull(storeService);

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("11111111111111");
        store.setEmail("abc@abc");
        store.setPhone("11111111111");
        store.setPassword("abc");

        storeService.persistStore(store);
        then(storeRepository).should(times(1)).save(any());

        Store store2 = new Store();
        store2.setCorporateName("Versnelt");
        store2.setCnpj("11111111111112");
        store2.setEmail("abc@123");
        store2.setPhone("11111111111");
        store2.setPassword("abc");

        when(storeRepository.findByPhone(store.getPhone())).thenReturn(Optional.of(store));
        var assertThrows = assertThrows(DuplicateKeyException.class,
                () -> storeService.persistStore(store2));

        assertEquals("O telefone já está sendo utilizado", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa busca de todas as lojas quando não há registros.")
    public void test_buscaTodasLojasQuandoVazio_lancaException() {
        assertNotNull(storeService);

        Page<Store> stores = new PageImpl<>(new ArrayList<>(), this.pageable, 0);
        when(storeRepository.findAll(pageable)).thenReturn(stores);

        var assertThrows = assertThrows(NotFoundException.class,
                () -> this.storeService.getAllStores(pageable));

        assertEquals("Nenhuma loja foi encontrada.", assertThrows.getMessage());
        then(storeRepository).should(times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Testa busca de todas as lojas quando há registros.")
    public void test_buscaTodasLojasQuandoHaRegistros() {
        assertNotNull(storeService);

        List<Store> stores = new ArrayList<>();

        for (int x = 0; x < 555; x++) {
            stores.add(new Store());
        }

        Page<Store> storesPage = new PageImpl<>(stores, this.pageable, 0);
        when(storeRepository.findAll(pageable)).thenReturn(storesPage);


        Page<Store> storePageResult = this.storeService.getAllStores(pageable);

        assertEquals(stores.size(), storePageResult.getSize());
        then(storeRepository).should(times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Testa busca de lojas pelo ID quando não existe.")
    public void test_buscaLojaPorIdQuandoNaoExiste_lancaException() {
        assertNotNull(storeService);

        when(storeRepository.findById(any())).thenReturn(Optional.empty());

        var assertThrows = assertThrows(NotFoundException.class,
                () -> storeService.getStoreByID(BigInteger.ONE));

        assertEquals("Nenhuma loja foi encontrada.", assertThrows.getMessage());
        then(storeRepository).should(times(1)).findById(any());
    }

    @Test
    @DisplayName("Testa busca de lojas pelo ID quando existe.")
    public void test_buscaLojaPorIdQuandoExiste() {
        assertNotNull(storeService);

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("11111111111111");
        store.setEmail("abc@abc");
        store.setPhone("11111111111");
        store.setPassword("abc");

        when(storeRepository.findById(any())).thenReturn(Optional.of(store));

        Store storeReturned = storeService.getStoreByID(BigInteger.ONE);

        assertEquals(store.getEmail(), storeReturned.getEmail());
        then(storeRepository).should(times(1)).findById(any());
    }

    @Test
    @DisplayName("Testa alteração de loja quando não existe.")
    public void test_alterLojaQuandoNaoExiste_lancaException() {
        assertNotNull(storeService);

        when(storeRepository.findByEmail(any())).thenReturn(Optional.empty());

        var assertThrows = assertThrows(NotFoundException.class,
                () -> storeService.putStore("a@a", new Store()));

        assertEquals("Nenhuma loja foi encontrada.", assertThrows.getMessage());
        then(storeRepository).should(times(1)).findByEmail(any());
    }

    @Test
    @DisplayName("Testa alteração de loja quando existe, mas os dados são inválidos.")
    public void test_alterLojaQuandoExisteMasValoresInvalidos_lancaException() {
        assertNotNull(storeService);

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("11111111111111");
        store.setEmail("abc@abc");
        store.setPhone("11111111111");
        store.setPassword("abc");

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(store));

        Store store2 = new Store();
        store2.setCorporateName("");
        store2.setCnpj("asv");
        store2.setEmail("abcabc");
        store2.setPhone("asd");
        store2.setPassword("");

        var assertThrows = assertThrows(ConstraintViolationException.class, () ->
                storeService.putStore("abc@abc", store2));

        List<String> messages = assertThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertEquals(6, assertThrows.getConstraintViolations().size());
        assertThat(messages, hasItems(
                "CNPJ inválido.",
                "A razão social não pode ser vazia.",
                "E-mail inválido.",
                "Número de telefone inválido.",
                "A senha não pode ser vazia.",
                "A senha é muito pequena."
        ));
    }

    @Test
    @DisplayName("Testa alteração de loja quando existe e com dados válidos.")
    public void test_alterLojaQuandoExisteComValoresValidos() {
        assertNotNull(storeService);

        Store store = new Store();
        store.setCorporateName("Versnelt");
        store.setCnpj("11111111111111");
        store.setEmail("abc@abc");
        store.setPhone("11111111111");
        store.setPassword("abc");

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(store));

        Store store2 = new Store();
        store2.setCorporateName("Cristiano");
        store2.setCnpj("11111111111122");
        store2.setEmail("abc@123");
        store2.setPhone("11111111333");
        store2.setPassword("123456");

        storeService.putStore("abc@abc", store2);

        assertEquals(store2.getCorporateName(), store.getCorporateName());
        assertEquals(store2.getCnpj(), store.getCnpj());
        assertEquals(store2.getEmail(), store.getEmail());
        then(storeRepository).should(times(1)).save(any());
        then(storeRepository).should(times(1)).findByEmail(any());
    }

    @Test
    @DisplayName("Testa delete de loja quando não existe.")
    public void test_deleteLojaQuandoNãoExiste_lancaException() {
        assertNotNull(storeService);

        when(storeRepository.findByEmail(any())).thenReturn(Optional.empty());

        var assertThrows = assertThrows(NotFoundException.class,
                () -> storeService.deleteStore("abc@abc"));

        assertEquals("Loja não encontrada.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa delete de loja quando existe, mas sem endereços e produtos.")
    public void test_deleteLojaQuandoExisteSemEnderecoOuProdutos() {
        assertNotNull(storeService);

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(new Store()));

        storeService.deleteStore("abc@abc");

        then(storeRepository).should(times(1)).delete(any());
    }

    @Test
    @DisplayName("Testa delete de loja quando existe, com endereços e produtos.")
    public void test_deleteLojaQuandoExisteComEnderecosEProdutos() {
        assertNotNull(storeService);

        Set<AddressStore> addresses = new HashSet<>();

        for(int x=0; x<100; x++) {
            addresses.add(new AddressStore());
        }

        Set<Product> products = new HashSet<>();

        for(int x=0; x<100; x++) {
            Product product = new Product();
            product.setCode(String.valueOf(x));
            products.add(product);
        }

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(new Store()));
        when(addressStoreRepository.findAddressesByStore(any())).thenReturn(Optional.of(addresses));
        when(productRepository.findProductsByStore(any())).thenReturn(Optional.of(products));

        storeService.deleteStore("abc@abc");

        then(storeRepository).should(times(1)).delete(any());
        then(productRepository).should(times(products.size())).delete(any());
        then(addressStoreRepository).should(times(addresses.size())).delete(any());
    }
}