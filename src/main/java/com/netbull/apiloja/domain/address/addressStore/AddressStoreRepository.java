package com.netbull.apiloja.domain.address.addressStore;

import com.netbull.apiloja.domain.store.Store;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AddressStoreRepository extends CrudRepository<AddressStore, BigInteger> {
    public Optional<Set<AddressStore>> findAddressesByStore(Store store);
}
