package com.netbull.apiloja.controller;

import com.netbull.apiloja.domain.store.Store;
import com.netbull.apiloja.service.StoreService;
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
import java.math.BigInteger;
import java.net.URI;

@Controller
@RequestMapping(path = "/v1/stores")
public class StoreController {

    @Autowired
    StoreService storeService;

    @Operation(summary = "Cria uma loja.")
    @PostMapping(produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> createStore(@RequestBody Store store) {
        this.storeService.persistStore(store);

        URI uri = MvcUriComponentsBuilder.fromController(getClass())
                .path("/{id}")
                .buildAndExpand(store.getId())
                .toUri();

        return ResponseEntity.created(uri).body("Loja criada.");
    }

    @Operation(summary = "Busca loja pelo ID.")
    @GetMapping(path = "/{id}" ,produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<Store> getStoreById(@PathVariable BigInteger id) {
        return ResponseEntity.ok(this.storeService.getStoreByID(id));
    }

    @Operation(summary = "Busca loja pelo e-mail.")
    @GetMapping(path = "/email/{email}" ,produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<Store> getStoreByCnpj(@PathVariable String email) {
        return ResponseEntity.ok(this.storeService.getStoreByEmail(email));
    }

    @Operation(summary = "Busca todas as lojas cadastradas.")
    @GetMapping(produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<Page<Store>> getAllStores(
            @ParameterObject @PageableDefault(sort = {"id"},
                    direction = Sort.Direction.ASC,
            page = 0, size = 10)
                    Pageable pageable) {
        return ResponseEntity.ok(this.storeService.getAllStores(pageable));
    }

    @Operation(summary = "Altera uma loja.")
    @PutMapping(produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> alterStore(@RequestBody Store store) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        this.storeService.putStore(auth.getName() ,store);

        URI uri = MvcUriComponentsBuilder.fromController(getClass())
                .path("/{id}")
                .buildAndExpand(store.getId())
                .toUri();

        return ResponseEntity.created(uri).body("Loja alterada.");
    }

    @Operation(summary = "Exclui uma loja.")
    @DeleteMapping(produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> deleteStore() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        this.storeService.deleteStore(auth.getName());

        return ResponseEntity.ok("Loja deletada.");
    }
}
