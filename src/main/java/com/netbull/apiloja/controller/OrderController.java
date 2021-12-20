package com.netbull.apiloja.controller;

import com.netbull.apiloja.domain.order.Order;
import com.netbull.apiloja.domain.order.OrderState;
import com.netbull.apiloja.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
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

import javax.ws.rs.core.MediaType;
import java.math.BigInteger;

@RestController
@Controller
@Slf4j
@RequestMapping(path = "/v1/stores/orders")
public class OrderController {

    @Autowired
    OrderService orderService;

    @Operation(summary = "Buscar todos os pedidos da loja.")
    @GetMapping( produces = {MediaType.APPLICATION_JSON})
    public ResponseEntity<Page<Order>> getAllOrdersByStore(
            @ParameterObject @PageableDefault(sort = {"id"}, direction = Sort.Direction.ASC,
            page = 0, size = 10) Pageable pageable) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Page<Order> order = orderService.getOrdersPageByStore(pageable, auth.getName());

        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Alterar estado do pedido para enviado.")
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> patchAddressType(@PathVariable BigInteger id, @RequestBody Order order) {
        OrderState orderState = order.getState();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        this.orderService.setOrderStateToDispatched(id, auth.getName(), orderState);

        return ResponseEntity.ok("Pedido alterado para enviado.");
    }
}
