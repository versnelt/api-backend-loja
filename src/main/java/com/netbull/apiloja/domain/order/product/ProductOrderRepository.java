package com.netbull.apiloja.domain.order.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;

@Repository
public interface ProductOrderRepository extends JpaRepository<ProductOrder, BigInteger> {
    public Optional<ProductOrder> findProductOrderByCode(String code);
}
