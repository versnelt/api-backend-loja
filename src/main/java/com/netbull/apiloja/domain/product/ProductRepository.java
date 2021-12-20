package com.netbull.apiloja.domain.product;

import com.netbull.apiloja.domain.store.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface ProductRepository extends PagingAndSortingRepository<Product, BigInteger> {
    public Optional<Product> findProductsByStoreAndCode(Store store, String code);
    public Optional<Set<Product>> findProductsByStore(Store store);
    public default Page<Product> findProductsByStorePage(Pageable pageable, Store store) {
        List<Product> products = this.findProductsByStore(store).orElse(new HashSet<Product>())
                .stream()
                .collect(Collectors.toList());
        return new PageImpl<Product>(products, pageable, products.size());
    }
}
