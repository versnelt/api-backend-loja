package com.netbull.apiloja.controller;

import com.netbull.apiloja.domain.product.Product;
import com.netbull.apiloja.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;

@Controller
@RequestMapping(path = "/v1/stores/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Operation(summary = "Criar um produto.")
    @PostMapping(produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> persistProduct(@RequestBody Product product) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        productService.persitProduct(product, auth.getName());

        URI uri = MvcUriComponentsBuilder.fromController(getClass())
                .path("/{id}")
                .buildAndExpand(product.getId())
                .toUri();

        return ResponseEntity.created(uri).body("Produto criado.");
    }

    @Operation(summary = "Buscar um produto.")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<Product> getProductById(@PathVariable BigInteger id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(productService.getProductById(id, auth.getName()));
    }

    @Operation(summary = "Buscar todos os produto.")
    @GetMapping(produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<Page<Product>> getAllProductsByStore(
            @ParameterObject @PageableDefault(sort = {"id"},
                    direction = Sort.Direction.ASC,
                    page = 0, size = 10)
                    Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(productService.getProductsByStoreEmail(pageable, auth.getName()));
    }

    @GetMapping(path = "/store-id/{id}", produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<Page<Product>> getAllProductsByStoreId(
            @ParameterObject @PageableDefault(sort = {"id"},
                    direction = Sort.Direction.ASC,
                    page = 0, size = 10)
                    Pageable pageable,
            @PathVariable BigInteger id) {
        return ResponseEntity.ok(productService.getProductsByStoreId(pageable, id));
    }

    @Operation(summary = "Alterar um preço de um produto.")
    @PatchMapping(path = "/{id}/price/{price}", produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> alterProductPrice(@PathVariable BigInteger id,
                                                    @PathVariable BigDecimal price) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        productService.patchProductPrice(id, auth.getName(), price);

        URI uri = MvcUriComponentsBuilder.fromController(getClass())
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();

        return ResponseEntity.created(uri).body("Preço alterado.");
    }

    @Operation(summary = "Alterar um produto.")
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> alterProduct(@PathVariable BigInteger id,
                                               @RequestBody Product product) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        productService.putProduct(id, auth.getName(), product);

        URI uri = MvcUriComponentsBuilder.fromController(getClass())
                .path("{id}")
                .buildAndExpand(id)
                .toUri();

        return ResponseEntity.created(uri).body("Produto alterado.");
    }

    @Operation(summary = "Deletar um produto.")
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> deleteProduct(@PathVariable BigInteger id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        productService.deleteById(id, auth.getName());

        return ResponseEntity.ok("Produto deletado.");
    }
}