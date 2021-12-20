package com.netbull.apiloja.service;

import com.netbull.apiloja.domain.address.addressStore.AddressStoreRepository;
import com.netbull.apiloja.domain.product.ProductRepository;
import com.netbull.apiloja.domain.store.Store;
import com.netbull.apiloja.domain.store.StoreRepository;
import com.netbull.apiloja.utility.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.ws.rs.NotFoundException;
import java.math.BigInteger;
import java.util.Set;

@Service
@Slf4j
public class StoreService {

    private StoreRepository storeRepository;

    private ProductRepository productRepository;

    private AddressStoreRepository addressStoreRepository;

    private Validator validator;

    private StringUtils stringUtils;

    private RabbitTemplate rabbitTemplate;

    public StoreService(Validator validator, StoreRepository storeRepository,
                        ProductRepository productRepository, AddressStoreRepository addressStoreRepository,
                        StringUtils stringUtils, RabbitTemplate rabbitTemplate) {
        this.validator = validator;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.addressStoreRepository = addressStoreRepository;
        this.stringUtils = stringUtils;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void persistStore(@NotNull(message = "Loja não pode ser nula.") Store store) {

        Set<ConstraintViolation<Store>> validator = this.validator.validate(store);

        if(!validator.isEmpty()) {
            throw new ConstraintViolationException("Loja inválida.", validator);
        }

        if(storeRepository.findByCnpj(store.getCnpj()).isPresent()) {
            throw new DuplicateKeyException("O CNPJ já está sendo utilizado");
        }

        if(storeRepository.findByEmail(store.getEmail()).isPresent()) {
            throw new DuplicateKeyException("O e-mail já está sendo utilizado");
        }

        if(storeRepository.findByPhone(store.getPhone()).isPresent()) {
            throw new DuplicateKeyException("O telefone já está sendo utilizado");
        }

        store.setPassword(stringUtils.encryptPassword(store.getPassword()));

        if (this.storeRepository.save(store) != null) {
            log.info("Loja criada: {}", store.getId());
        }
        this.rabbitTemplate.convertAndSend("store", "store.created" ,store);
    }

    public Page<Store> getAllStores(Pageable pageable) {
        Page<Store> stores = storeRepository.findAll(pageable);

        if(stores.isEmpty()) {
            throw new NotFoundException("Nenhuma loja foi encontrada.");
        }

        return stores;
    }

    public Store getStoreByID(BigInteger id) {
        return storeRepository.findById(id).orElseThrow(() -> new NotFoundException("Nenhuma loja foi encontrada."));
    }

    public Store getStoreByEmail(String email) {
        return storeRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("Nenhuma loja foi encontrada."));
    }

    @Transactional
    public void putStore(String userEmail, Store newStore) {
        Store oldStore = storeRepository.findByEmail(userEmail).orElseThrow(
                () -> new NotFoundException("Nenhuma loja foi encontrada."));

        Set<ConstraintViolation<Store>> validator = this.validator.validate(newStore);

        if(!validator.isEmpty()) {
            throw new ConstraintViolationException("Loja inválida.", validator);
        }

        oldStore.setCorporateName(newStore.getCorporateName());
        oldStore.setPhone(newStore.getPhone());
        oldStore.setCnpj(newStore.getCnpj());
        oldStore.setEmail(newStore.getEmail());
        oldStore.setPassword(newStore.getPassword());

        newStore.setId(oldStore.getId());

        if (this.storeRepository.save(oldStore) != null) {
            log.info("Loja alterada: {}", oldStore.getId());
        }
        this.rabbitTemplate.convertAndSend("store", "store.updated", oldStore);
    }

    @Transactional
    public void deleteStore(String userEmail) {
        Store store = storeRepository.findByEmail(userEmail).orElseThrow(
                () -> new NotFoundException("Loja não encontrada."));

        addressStoreRepository.findAddressesByStore(store)
                        .ifPresent(setAddress -> setAddress.forEach(addressStoreRepository::delete));

        productRepository.findProductsByStore(store)
                        .ifPresent(setProducts -> setProducts.forEach(productRepository::delete));

        storeRepository.delete(store);
        log.info("Loja deletada: {}", store.getId());
        this.rabbitTemplate.convertAndSend("store", "store.deleted" ,store);
    }
}
