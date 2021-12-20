package com.netbull.apiloja.service;

import com.netbull.apiloja.domain.product.Product;
import com.netbull.apiloja.domain.product.ProductRepository;
import com.netbull.apiloja.domain.store.Store;
import com.netbull.apiloja.domain.store.StoreRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;

import javax.validation.*;
import javax.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductServiceTest {

    private ProductService productService;

    private StoreRepository storeRepository;

    private ProductRepository productRepository;

    private Validator validator;

    private Pageable pageable;

    private RabbitTemplate rabbitTemplate;

    @BeforeAll
    public void setupBeforAll() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        this.validator = validatorFactory.getValidator();
    }

    @BeforeEach
    public void setupBeforeEach() {
        this.storeRepository = Mockito.mock(StoreRepository.class);
        this.productRepository = Mockito.mock(ProductRepository.class);
        this.pageable = Mockito.mock(Pageable.class);
        this.rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        this.productService = new ProductService(productRepository, storeRepository, validator, rabbitTemplate);
    }

    @Test
    @DisplayName("Testa persistir quando produto nulo.")
    public void test_persistQuandoProductNull_lancaException() {
        assertNotNull(productService);

        Product product = null;

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> productService.persitProduct(product, "a@a"));
        assertEquals("O produto não pode ser nulo.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa persistir quando não encontra loja.")
    public void test_persistQuandoNaoEncontraLoja_lancaException() {
        assertNotNull(productService);

        Product product = new Product();

        var assertThrows = assertThrows(NotFoundException.class,
                () -> productService.persitProduct(product, "a@a"));

        assertEquals("Loja não encontrada.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa persistir quando já existe um produto com o mesmo código.")
    public void test_persistQuandoJaExisteProdutoComMesmoCodigo_lancaException() {
        assertNotNull(productService);

        Product product = new Product();

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(new Store()));
        when(productRepository.findProductsByStoreAndCode(any(), any())).thenReturn(Optional.of(new Product()));

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> productService.persitProduct(product, "a@a"));

        assertEquals("Já existe um produto com o mesmo código.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa persistir quando produto tem campos nulos.")
    public void test_persistQuandoProductTemCamposNull_lancaException() {
        assertNotNull(productService);

        Product product = new Product();

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(new Store()));

        var assertThrows = assertThrows(ConstraintViolationException.class,
                () -> productService.persitProduct(product, "a@a"));

        assertEquals(4, assertThrows.getConstraintViolations().size());

        List<String> messages = assertThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "O nome do produto não pode ser vazio.",
                "O preço não pode ser vazio.",
                "A quantidade de produtos não pode ser vazia.",
                "O código do produto não pode ser vazio."
        ));
    }

    @Test
    @DisplayName("Testa persistir quando produto tem campos fora do padrão.")
    public void test_persistQuandoProductTemCamposForaDoPadrao_lancaException() {
        assertNotNull(productService);

        StringBuilder name = new StringBuilder();
        for (int x = 0; x < 100; x++) {
            name.append("a");
        }

        StringBuilder description = new StringBuilder();
        for (int x = 0; x < 1000; x++) {
            description.append("a");
        }

        Product product = new Product();
        product.setName(name.toString());
        product.setDescription(description.toString());
        product.setPrice(BigDecimal.valueOf(-1));
        product.setQuantity(BigInteger.valueOf(-1));
        product.setCode("");

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(new Store()));

        var assertThrows = assertThrows(ConstraintViolationException.class,
                () -> productService.persitProduct(product, "a@a"));

        assertEquals(5, assertThrows.getConstraintViolations().size());

        List<String> messages = assertThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "O nome do produto é muito grande.",
                "O preço não pode ser menor que R$0,00.",
                "A descrição do produto é muito grande.",
                "A quantidade não pode ser menor que 0.",
                "O código do produto não pode ser vazio."
        ));
    }

    @Test
    @DisplayName("Testa persistir quando produto tem campos dentro padrão.")
    public void test_persistQuandoProductPadrao() {
        assertNotNull(productService);

        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51");

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(new Store()));

        productService.persitProduct(product, "a@a");
        then(productRepository).should(times(1)).save(any());
        then(rabbitTemplate).should(times(1)).convertAndSend(anyString(), anyString(), eq(product));
    }

    @Test
    @DisplayName("Testa busca produtos quando há registros.")
    public void test_buscaTodaosProdutosQuandoHaRegistro() {
        assertNotNull(productService);

        List<Product> products = new ArrayList<>();

        for (int x = 0; x < 555; x++) {
            products.add(new Product());
        }

        Page<Product> productsPage = new PageImpl<>(products, this.pageable, 0);

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(new Store()));
        when(productRepository.findProductsByStorePage(any(), any())).thenReturn(productsPage);

        Page<Product> productsPageResult = this.productService.getProductsByStoreEmail(pageable, "a@a");

        assertEquals(products.size(), productsPageResult.getSize());
        then(productRepository).should(times(1)).findProductsByStorePage(any(), any());
    }

    @Test
    @DisplayName("Testa busca produtos quando não há registros.")
    public void test_buscaTodaosProdutosQuandoNaoHaRegistro_lancaException() {
        assertNotNull(productService);

        List<Product> products = new ArrayList<>();

        Page<Product> productsPage = new PageImpl<>(products, this.pageable, 0);

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(new Store()));
        when(productRepository.findProductsByStorePage(any(), any())).thenReturn(productsPage);

        var assertThrows = assertThrows(NotFoundException.class,
                () -> this.productService.getProductsByStoreEmail(pageable, "a@a"));

        assertEquals("Nenhum produto foi encontrado.", assertThrows.getMessage());
        then(productRepository).should(times(1)).findProductsByStorePage(any(), any());
    }

    @Test
    @DisplayName("Testa busca produtos quando loja não é encontrada.")
    public void test_buscaTodosProdutosQuandoLojaNaoEncontrada_lancaException() {
        assertNotNull(productService);

        when(storeRepository.findByEmail(any())).thenReturn(Optional.empty());

        var assertThrows = assertThrows(NotFoundException.class,
                () -> this.productService.getProductsByStoreEmail(pageable, "a@a"));

        assertEquals("Loja não encontrada.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa busca produto pelo ID.")
    @WithMockUser("a@a")
    public void test_buscaProdutosId() {
        assertNotNull(productService);

        Store store = new Store();
        store.setEmail("a@a");

        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51");
        product.setStore(store);

        when(productRepository.findById(any())).thenReturn(Optional.of(product));

        Product productResult = productService.getProductById(BigInteger.ONE, store.getEmail());

        assertEquals(product.getDescription(), productResult.getDescription());
        then(productRepository).should(times(1)).findById(any());
    }

    @Test
    @DisplayName("Testa busca produto pelo ID quando o produto não pertence à loja logada.")
    @WithMockUser("a@a")
    public void test_buscaProdutosIdQuandoNaoPertenceALoja_lancaException() {
        assertNotNull(productService);

        Store store = new Store();
        store.setEmail("a@ab");

        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51");
        product.setStore(store);

        when(productRepository.findById(any())).thenReturn(Optional.of(product));

        var assertThrows = assertThrows(NotFoundException.class,
                () -> this.productService.getProductById(BigInteger.ONE, "a@a"));

        assertEquals("Nenhum produto foi encontrado.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa busca produto pelo ID quando não encontra.")
    public void test_buscaProdutoId_lancaException() {
        assertNotNull(productService);

        when(productRepository.findById(any())).thenReturn(Optional.empty());

        var assertThrows = assertThrows(NotFoundException.class,
                () -> this.productService.getProductById(BigInteger.ONE, "a@a"));

        assertEquals("Nenhum produto foi encontrado.", assertThrows.getMessage());
        then(productRepository).should(times(1)).findById(any());
    }

    @Test
    @DisplayName("Testa busca de todos produtos da loja, pelo ID da loja, quando não existe.")
    public void test_buscaTodosProdutosDaLojaQuandoNaoExiste_lancaException() {
        assertNotNull(productService);

        var assertThrows = assertThrows(NotFoundException.class,
                () -> this.productService.getProductsByStoreId(pageable, BigInteger.ONE));

        assertEquals("Loja não encontrada.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa busca de todos produtos da loja, pelo ID da loja, quando não encontra produtos.")
    public void test_buscaTodosProdutosDaLojaQuandoNaoEncontraProdutos_lancaException() {
        assertNotNull(productService);

        when(storeRepository.findById(any())).thenReturn(Optional.of(new Store()));
        when(productRepository.findProductsByStorePage(any(), any())).thenReturn(Page.empty());

        var assertThrows = assertThrows(NotFoundException.class,
                () -> this.productService.getProductsByStoreId(pageable, BigInteger.ONE));

        assertEquals("Nenhum produto foi encontrado.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa busca de todos produtos da loja, pelo ID da loja, quando encontra produtos.")
    public void test_buscaTodosProdutosDaLojaQuandoEncontraProdutos() {
        assertNotNull(productService);

        List<Product> products = new ArrayList<>();
        products.add(new Product());
        Page<Product> productsPage = new PageImpl<>(products, this.pageable, 0);

        when(storeRepository.findById(any())).thenReturn(Optional.of(new Store()));
        when(productRepository.findProductsByStorePage(any(), any())).thenReturn(productsPage);

        Page<Product> productPageResult = productService.getProductsByStoreId(pageable, BigInteger.ONE);

        assertEquals(products.size(), productPageResult.getSize());
    }

    @Test
    @DisplayName("Testa alteração do preço do produto quando menor que zero.")
    public void test_patchProdutoQuandoMenorQueZero_lancaException() {
        assertNotNull(productService);

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> productService.patchProductPrice(BigInteger.ONE, "a@a",BigDecimal.valueOf(-1)));
        assertEquals("O preço não pode ser negativo.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do preço do produto quando não encontra loja.")
    public void test_patchProdutoQuandoNaoEncontraLoja_lancaException() {
        assertNotNull(productService);

        var assertThrows = assertThrows(NotFoundException.class,
                () -> productService.patchProductPrice(BigInteger.ONE, "a@a",BigDecimal.valueOf(1)));
        assertEquals("Loja não encontrada.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do preço do produto quando não encontra produto.")
    public void test_patchProdutoQuandoNaoEncontraProduto_lancaException() {
        assertNotNull(productService);

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(new Store()));

        var assertThrows = assertThrows(NotFoundException.class,
                () -> productService.patchProductPrice(BigInteger.ONE, "a@a",BigDecimal.valueOf(1)));
        assertEquals("O produto não foi encontrado.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do preço do produto quando não encontra produto associado à loja.")
    public void test_patchProdutoQuandoNaoEncontraProdutoAssociadoALoja_lancaException() {
        assertNotNull(productService);

        Store store = new Store();
        store.setId(BigInteger.ONE);

        Store store2 = new Store();
        store2.setId(BigInteger.TWO);

        Product product = new Product();
        product.setStore(store2);

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(store));
        when(productRepository.findById(any())).thenReturn(Optional.of(product));

        var assertThrows = assertThrows(NotFoundException.class,
                () -> productService.patchProductPrice(BigInteger.ONE, "a@a",BigDecimal.valueOf(1)));
        assertEquals("O produto não foi encontrado.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do preço do produto.")
    public void test_patchProduto() {
        assertNotNull(productService);

        Store store = new Store();
        store.setId(BigInteger.ONE);

        Product product = new Product();
        product.setStore(store);

        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(store));
        when(productRepository.findById(any())).thenReturn(Optional.of(product));

        productService.patchProductPrice(BigInteger.ONE, "a@a", BigDecimal.ONE);

        assertEquals(BigDecimal.ONE, product.getPrice());
        then(productRepository).should(times(1)).save(any());
    }

    @Test
    @DisplayName("Testa alteração de produto pelo ID quando não encontra.")
    public void test_alterProduto_lancaException() {
        assertNotNull(productService);

        when(productRepository.findById(any())).thenReturn(Optional.empty());
        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(new Store()));

        var assertThrows = assertThrows(NotFoundException.class,
                () -> this.productService.putProduct(BigInteger.ONE, "a@a", new Product()));

        assertEquals("O produto não foi encontrado.", assertThrows.getMessage());
        then(productRepository).should(times(1)).findById(any());
    }

    @Test
    @DisplayName("Testa alteração de produto pelo ID quando o produto não está associado à loja.")
    public void test_alterProdutoQuandoProdutoNaoEstaAssociadoALoja_lancaException() {
        assertNotNull(productService);

        Store store = new Store();
        store.setId(BigInteger.ONE);

        Store store2 = new Store();
        store.setId(BigInteger.TWO);

        Product product = new Product();
        product.setStore(store2);

        when(productRepository.findById(any())).thenReturn(Optional.of(product));
        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(store));

        var assertThrows = assertThrows(NotFoundException.class,
                () -> this.productService.putProduct(BigInteger.ONE, "a@a", new Product()));

        assertEquals("O produto não foi encontrado.", assertThrows.getMessage());
        then(productRepository).should(times(1)).findById(any());
    }

    @Test
    @DisplayName("Testa alteração de produto pelo ID quando produto fora do padrão.")
    public void test_alterProdutoQuandoProdutoForaDoPadrão_lancaException() {
        assertNotNull(productService);

        Store store = new Store();
        store.setId(BigInteger.ONE);

        Store store2 = new Store();
        store2.setId(BigInteger.ONE);

        Product oldproduct = new Product();
        oldproduct.setStore(store2);

        StringBuilder name = new StringBuilder();
        for (int x = 0; x < 100; x++) {
            name.append("a");
        }

        StringBuilder description = new StringBuilder();
        for (int x = 0; x < 1000; x++) {
            description.append("a");
        }

        Product product = new Product();
        product.setName(name.toString());
        product.setDescription(description.toString());
        product.setPrice(BigDecimal.valueOf(-1));
        product.setQuantity(BigInteger.valueOf(-1));
        product.setCode("");

        when(productRepository.findById(any())).thenReturn(Optional.of(oldproduct));
        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(store));

        var assertThrows = assertThrows(ConstraintViolationException.class,
                () -> this.productService.putProduct(BigInteger.ONE, "a@a", product));

        assertEquals(5, assertThrows.getConstraintViolations().size());

        List<String> messages = assertThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "O nome do produto é muito grande.",
                "O preço não pode ser menor que R$0,00.",
                "A quantidade não pode ser menor que 0.",
                "O código do produto não pode ser vazio.",
                "A descrição do produto é muito grande."
        ));
    }

    @Test
    @DisplayName("Testa alteração de produto pelo ID.")
    public void test_alterProdutoQuandoProdutoForaNoPadrão() {
        assertNotNull(productService);

        Store store = new Store();
        store.setId(BigInteger.ONE);

        Store store2 = new Store();
        store2.setId(BigInteger.ONE);

        Product oldproduct = new Product();
        oldproduct.setStore(store2);

        Product product = new Product();
        product.setName("TV");
        product.setDescription("20polegadas");
        product.setPrice(BigDecimal.valueOf(1500));
        product.setQuantity(BigInteger.valueOf(20));
        product.setCode("1234A51");

        when(productRepository.findById(any())).thenReturn(Optional.of(oldproduct));
        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(store));

        this.productService.putProduct(BigInteger.ONE, "a@a", product);

        then(productRepository).should(times(1)).save(any());
        assertEquals(oldproduct.getDescription(), product.getDescription());
    }

    @Test
    @DisplayName("Testa delete quando não encontra o produto")
    public void test_deleteProductByIdQuandoProdutoNaoExiste_lancaException() {
        assertNotNull(productService);

        var assertThrows = assertThrows(NotFoundException.class,
                () -> productService.deleteById(BigInteger.ONE, "a@a"));

        assertEquals("O produto não foi encontrado.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa delete quando não produto não pertence à loja")
    public void test_deleteProductByIdQuandoProdutoNaoPertencaALoja_lancaException() {
        assertNotNull(productService);

        Store store = new Store();
        store.setId(BigInteger.ONE);

        Product product = new Product();
        product.setStore(store);

        Store store2 = new Store();
        store2.setId(BigInteger.TEN);

        when(productRepository.findById(any())).thenReturn(Optional.of(product));
        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(store2));

        var assertThrows = assertThrows(NotFoundException.class,
                () -> productService.deleteById(BigInteger.ONE, "a@a"));

        assertEquals("O produto não foi encontrado.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa delete de produto")
    public void test_deleteProductById() {
        assertNotNull(productService);

        Store store = new Store();
        store.setId(BigInteger.ONE);

        Product product = new Product();
        product.setStore(store);

        when(productRepository.findById(any())).thenReturn(Optional.of(product));
        when(storeRepository.findByEmail(any())).thenReturn(Optional.of(store));

        productService.deleteById(BigInteger.ONE, "a@a");

        then(productRepository).should(times(1)).deleteById(any());
    }
}