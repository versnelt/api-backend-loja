package com.netbull.apiloja.domain.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "store")
public class Store implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_store")
    @SequenceGenerator(name = "sequence_store",sequenceName = "sequence_store",
            allocationSize = 1,
            initialValue = 1)
    private BigInteger id;

    @NotBlank(message = "O CNPJ não pode ser vazio.")
    @Pattern(regexp = "[0-9]{14}", message = "CNPJ inválido.")
    private String cnpj;

    @NotBlank(message = "A razão social não pode ser vazia.")
    @Size(max = 50, message = "A razão social é muito grande.")
    private String corporateName;

    @NotBlank(message = "O e-mail não pode ser vazio.")
    @Email(message = "E-mail inválido.")
    private String email;

    @NotBlank(message = "O número de telefone não pode ser vazio.")
    @Pattern(regexp = "[0-9]{11}", message = "Número de telefone inválido.")
    private String phone;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "A senha não pode ser vazia.")
    @Size(min = 3, message = "A senha é muito pequena.")
    private String password;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Store store = (Store) o;
        return cnpj.equals(store.cnpj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cnpj);
    }
}
