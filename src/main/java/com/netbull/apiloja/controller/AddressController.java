package com.netbull.apiloja.controller;

import com.netbull.apiloja.domain.address.addressStore.AddressStore;
import com.netbull.apiloja.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.net.URI;
import java.util.Set;

@Controller
@RequestMapping(path = "/v1/stores/addresses")
public class AddressController {

    @Autowired
    AddressService addressService;

    @Operation(summary = "Criar um endereço para um cliente existente" )
    @PostMapping(produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> createAddress(@RequestBody AddressStore addressStore) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        this.addressService.persistAddress(addressStore, auth.getName());

        URI uri = MvcUriComponentsBuilder.fromController(getClass())
                .path("/{id}")
                .buildAndExpand(addressStore.getId())
                .toUri();

        return ResponseEntity.created(uri).body("Endereço criado.");
    }

    @Operation(summary = "Buscar os endereço de cliente")
    @GetMapping(produces = {MediaType.APPLICATION_JSON})
    public ResponseEntity<Set<AddressStore>> getAddressByStore() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Set<AddressStore> addresses = this.addressService.getAddressByStoreEmail(auth.getName());

        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Buscar um endereço por id")
    @GetMapping(path = "/{id}", produces = {MediaType.APPLICATION_JSON})
    public ResponseEntity<AddressStore> getAddressById(@PathVariable BigInteger id) {

        AddressStore addressStore = this.addressService.getAddressById(id);

        return ResponseEntity.ok(addressStore);
    }

    @Operation(summary = "Alterar um endereço")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> alterAddress(@PathVariable BigInteger id, @RequestBody AddressStore addressStore) {

        this.addressService.putAddress(id, addressStore);

        return ResponseEntity.ok("Endereço Alterado");

    }

    @Operation(summary = "Deletar um endereço")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<String> deleteAddress(@PathVariable BigInteger id) {

        addressService.deleteAddress(id);

        return ResponseEntity.ok("Endereço deletado");
    }
}
