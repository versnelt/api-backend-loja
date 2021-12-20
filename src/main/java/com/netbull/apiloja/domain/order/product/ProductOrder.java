package com.netbull.apiloja.domain.order.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.netbull.apiloja.domain.order.Order;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@Entity
@Table(name = "product_order")
public class ProductOrder implements Serializable {

    @Id
    private BigInteger id;

    private BigDecimal price;

    private BigInteger quantity;

    private String code;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductOrder that = (ProductOrder) o;
        return code.equals(that.code) && order.equals(that.order);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, order);
    }
}
