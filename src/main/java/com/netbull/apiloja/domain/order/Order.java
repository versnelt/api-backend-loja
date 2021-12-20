package com.netbull.apiloja.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.netbull.apiloja.domain.order.addressClient.AddressClient;
import com.netbull.apiloja.domain.order.client.Client;
import com.netbull.apiloja.domain.order.product.ProductOrder;
import com.netbull.apiloja.domain.store.Store;
import com.netbull.apiloja.utility.JsonLocalDateDeserializer;
import com.netbull.apiloja.utility.JsonLocalDateSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "order_client")
public class Order implements Serializable {

    @Id
    private BigInteger id;

    @Enumerated(EnumType.STRING)
    private OrderState state;

    @JsonSerialize(using = JsonLocalDateSerializer.class)
    @JsonDeserialize(using = JsonLocalDateDeserializer.class)
    private LocalDate orderCreated;

    @JsonSerialize(using = JsonLocalDateSerializer.class)
    @JsonDeserialize(using = JsonLocalDateDeserializer.class)
    private LocalDate orderDispatched;

    @JsonSerialize(using = JsonLocalDateSerializer.class)
    @JsonDeserialize(using = JsonLocalDateDeserializer.class)
    private LocalDate orderDelivered;

    private BigDecimal totalValue;

    @JsonIgnoreProperties({"client"})
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private AddressClient address;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @JsonIgnoreProperties({"order"})
    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<ProductOrder> products;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
