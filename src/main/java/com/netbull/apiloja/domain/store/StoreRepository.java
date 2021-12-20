package com.netbull.apiloja.domain.store;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;

@Repository
public interface StoreRepository extends PagingAndSortingRepository<Store, BigInteger> {

    public Optional<Store> findByCnpj(String cnpj);
    public Optional<Store> findByEmail(String email);
    public Optional<Store> findByPhone(String phone);
    public Page<Store> findAll(Pageable pageable);
}
