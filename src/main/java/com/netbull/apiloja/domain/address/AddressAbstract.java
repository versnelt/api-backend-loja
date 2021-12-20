package com.netbull.apiloja.domain.address;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

@Setter
@Getter
@MappedSuperclass
public abstract class AddressAbstract implements Serializable {

    @NotBlank(message = "A rua não pode ser vazia.")
    private String street;

    @NotNull(message = "O número não pode ser vazio.")
    @Pattern(regexp = "[0-9]{1,9}", message = "Número inválido.")
    private String number;

    @NotBlank(message = "O bairro não pode ser vazio.")
    private String district;

    @NotBlank(message = "A cidade não pode ser vazia.")
    private String city;

    @NotBlank(message = "O CEP não pode ser vazio.")
    @Pattern(regexp = "[0-9]{8}", message = "CEP inválido.")
    private String cep;

    @NotBlank(message = "O Estado não pode ser vazio.")
    private String state;
}
