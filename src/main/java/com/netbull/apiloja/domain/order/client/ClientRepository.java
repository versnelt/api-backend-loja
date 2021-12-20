package com.netbull.apiloja.domain.order.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, BigInteger> {

    public Optional<Client> findByEmail(String email);
    public Optional<Client> findByCpf(String cpf);
}
