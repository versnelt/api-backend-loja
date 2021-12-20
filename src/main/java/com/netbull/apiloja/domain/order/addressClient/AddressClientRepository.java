package com.netbull.apiloja.domain.order.addressClient;

import com.netbull.apiloja.domain.order.client.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AddressClientRepository extends JpaRepository<AddressClient, BigInteger> {

    public Optional<Set<AddressClient>> findByClient(Client client);
}
