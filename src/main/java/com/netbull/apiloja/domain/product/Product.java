package com.netbull.apiloja.domain.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.netbull.apiloja.domain.store.Store;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "product_store")
public class Product implements Serializable {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_product_store")
    @SequenceGenerator(name = "sequence_product_store",sequenceName = "sequence_product_store",
            allocationSize = 1,
            initialValue = 1)
    private BigInteger id;

    @NotBlank(message = "O nome do produto não pode ser vazio.")
    @Size(max = 50, message = "O nome do produto é muito grande.")
    private String name;

    @Size(max = 500, message = "A descrição do produto é muito grande.")
    private String description;

    @NotNull(message = "O preço não pode ser vazio.")
    @Min(value = 0, message = "O preço não pode ser menor que R$0,00.")
    private BigDecimal price;

    @NotNull(message = "A quantidade de produtos não pode ser vazia.")
    @Min(value = 0, message = "A quantidade não pode ser menor que 0.")
    private BigInteger quantity;

    @NotBlank(message = "O código do produto não pode ser vazio.")
    private String code;

    @JsonIgnoreProperties({"cnpj", "corporateName", "email", "phone", "password"})
    @NotNull(message = "Loja não pode ser nula.")
    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return code.equals(product.code) && store.equals(product.store);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, store);
    }
}
