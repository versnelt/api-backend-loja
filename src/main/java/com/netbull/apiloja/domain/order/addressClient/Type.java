package com.netbull.apiloja.domain.order.addressClient;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "address_type")
public class Type implements Serializable {

    @Id
    private Integer id;

    private String description;
}