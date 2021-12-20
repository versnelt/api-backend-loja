package com.netbull.apiloja.domain.order.addressClient;

import com.netbull.apiloja.domain.address.AddressAbstract;
import com.netbull.apiloja.domain.order.client.Client;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigInteger;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "address_client")
public class AddressClient extends AddressAbstract implements Serializable {

    @Id
    private BigInteger id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "type_id")
    private Type type;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "client_id")
    private Client client;
}
