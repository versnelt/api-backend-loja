package com.netbull.apiloja.service;

import com.netbull.apiloja.domain.product.Product;
import com.netbull.apiloja.domain.product.ProductRepository;
import com.netbull.apiloja.domain.store.Store;
import com.netbull.apiloja.domain.store.StoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class ProductService {

    private ProductRepository productRepository;

    private StoreRepository storeRepository;

    private Validator validator;

    private RabbitTemplate rabbitTemplate;

    public ProductService(ProductRepository productRepository, StoreRepository storeRepository,
                          Validator validator, RabbitTemplate rabbitTemplate) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.validator = validator;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void persitProduct(Product product, String userEmail) {

        Optional.ofNullable(product).orElseThrow(
                () -> new IllegalArgumentException("O produto não pode ser nulo."));

        Store store = storeRepository.findByEmail(userEmail).orElseThrow(
                () -> new NotFoundException("Loja não encontrada."));

        if(productRepository.findProductsByStoreAndCode(store, product.getCode()).isPresent()) {
            throw new IllegalArgumentException("Já existe um produto com o mesmo código.");
        }

        product.setStore(store);

        Set<ConstraintViolation<Product>> validator = this.validator.validate(product);

        if(!validator.isEmpty()) {
            throw new ConstraintViolationException("Produto inválido.", validator);
        }

        if (productRepository.save(product) != null) {
            log.info("Produto criado: {}", product.getId());
        }
        this.rabbitTemplate.convertAndSend("product", "product.created" ,product);
    }

    public Product getProductById(BigInteger id, String userEmail) {

        Product product = productRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Nenhum produto foi encontrado."));

        if(!product.getStore().getEmail().equals(userEmail)){
            throw new NotFoundException("Nenhum produto foi encontrado.");
        }

        return product;
    }

    public Page<Product> getProductsByStoreEmail(Pageable pageable, String userEmail) {

        Store store = storeRepository.findByEmail(userEmail).orElseThrow(
                () -> new NotFoundException("Loja não encontrada."));

        Page<Product> products = productRepository.findProductsByStorePage(pageable, store);

        if(products.isEmpty()) {
            throw new NotFoundException("Nenhum produto foi encontrado.");
        }

        return products;
    }

    public Page<Product> getProductsByStoreId(Pageable pageable, BigInteger id) {

        Store store = storeRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Loja não encontrada."));

        Page<Product> products = productRepository.findProductsByStorePage(pageable, store);

        if(products.isEmpty()) {
            throw new NotFoundException("Nenhum produto foi encontrado.");
        }

        return products;
    }

    @Transactional
    public void patchProductPrice(BigInteger id, String userEmail, BigDecimal price) {
        if(BigDecimal.ZERO.compareTo(price) == 1) {
            throw new IllegalArgumentException("O preço não pode ser negativo.");
        }

        Store store = storeRepository.findByEmail(userEmail).orElseThrow(
                () -> new NotFoundException("Loja não encontrada."));

        Product product = productRepository.findById(id).orElseThrow(
                () -> new NotFoundException("O produto não foi encontrado."));

        if(product.getStore().getId() != store.getId()) {
            throw new NotFoundException("O produto não foi encontrado.");
        }

        product.setPrice(price);

        if (productRepository.save(product) != null) {
            log.info("Produto alterado: {}", product.getId());
        }
        this.rabbitTemplate.convertAndSend("product", "product.updated" ,product);
    }

    @Transactional
    public void putProduct(BigInteger id, String userEmail, Product newProduct) {
        Store store = storeRepository.findByEmail(userEmail).orElseThrow(
                () -> new NotFoundException("Loja não encontrada."));

        Product oldProduct = productRepository.findById(id).orElseThrow(
                () -> new NotFoundException("O produto não foi encontrado."));

        if(oldProduct.getStore().getId() != store.getId()) {
            throw new NotFoundException("O produto não foi encontrado.");
        }

        newProduct.setStore(oldProduct.getStore());

        Set<ConstraintViolation<Product>> validator = this.validator.validate(newProduct);

        if(!validator.isEmpty()) {
            throw new ConstraintViolationException("Produto inválido.", validator);
        }

        oldProduct.setName(newProduct.getName());
        oldProduct.setDescription(newProduct.getDescription());
        oldProduct.setPrice(newProduct.getPrice());
        oldProduct.setQuantity(newProduct.getQuantity());
        oldProduct.setCode(newProduct.getCode());

        if (productRepository.save(oldProduct) != null) {
            log.info("Produto alterado: {}", oldProduct.getId());
        }
        this.rabbitTemplate.convertAndSend("product", "product.updated" , oldProduct);
    }

    @Transactional
    public void deleteById(BigInteger id, String username) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new NotFoundException("O produto não foi encontrado."));

        Store store = storeRepository.findByEmail(username).orElseThrow(
                () -> new NotFoundException("Loja não encontrada."));

        if(product.getStore().getId() != store.getId()) {
            throw new NotFoundException("O produto não foi encontrado.");
        }

        this.rabbitTemplate.convertAndSend("product", "product.deleted" ,product);
        productRepository.deleteById(id);
        log.info("Produto deletado: {}", id);
    }
}
