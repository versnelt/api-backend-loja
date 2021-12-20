package com.netbull.apiloja.domain.address.addressStore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netbull.apiloja.domain.address.AddressAbstract;
import com.netbull.apiloja.domain.store.Store;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigInteger;

@Setter
@Getter
@Entity
@Table(name = "address_store")
public class AddressStore extends AddressAbstract implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_address_store")
    @SequenceGenerator(name = "sequence_address_store",sequenceName = "sequence_address_store",
            allocationSize = 1,
            initialValue = 1)
    private BigInteger id;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "A loja não pode ser nula.")
    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;
}
