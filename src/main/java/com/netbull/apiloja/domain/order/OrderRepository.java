package com.netbull.apiloja.domain.order;

import com.netbull.apiloja.domain.store.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface OrderRepository extends PagingAndSortingRepository<Order, BigInteger> {

    public Optional<Set<Order>> findOrdersByStore(Store store);

    public default Page<Order> findOrdersPageByStore(Pageable pageable, Store store) {
        List<Order> products = this.findOrdersByStore(store).orElse(new HashSet<Order>())
                .stream()
                .collect(Collectors.toList());
        return new PageImpl<Order>(products, pageable, products.size());
    }
}
