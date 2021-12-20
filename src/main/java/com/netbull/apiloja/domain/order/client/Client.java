package com.netbull.apiloja.domain.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.netbull.apiloja.utility.JsonLocalDateDeserializer;
import com.netbull.apiloja.utility.JsonLocalDateSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "client")
public class Client implements Serializable {

    @Id
    private BigInteger id;

    private String name;

    private String cpf;

    private String email;

    @JsonSerialize(using = JsonLocalDateSerializer.class)
    @JsonDeserialize(using = JsonLocalDateDeserializer.class)
    private LocalDate birthday;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return cpf.equals(client.cpf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpf);
    }
}
