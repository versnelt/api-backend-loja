package com.netbull.apiloja.service;

import com.netbull.apiloja.domain.address.addressStore.AddressStore;
import com.netbull.apiloja.domain.address.addressStore.AddressStoreRepository;
import lombok.extern.slf4j.Slf4j;
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
public class AddressService {

    private AddressStoreRepository addressStoreRepository;

    private Validator validator;

    private StoreService storeService;

    public AddressService(Validator validator, AddressStoreRepository addressStoreRepository,
                          StoreService storeService) {
        this.addressStoreRepository = addressStoreRepository;
        this.storeService = storeService;
        this.validator = validator;
    }

    @Transactional
    public void persistAddress(@NotNull(message = "Endereço não pode ser nulo.")
                                           AddressStore addressStore, String useremail) {

        addressStore.setStore(storeService.getStoreByEmail(useremail));

        Set<ConstraintViolation<AddressStore>> validate = this.validator.validate(addressStore);

        if (!validate.isEmpty()) {
            throw new ConstraintViolationException("Endereço inválido!", validate);
        }

        if (addressStoreRepository.save(addressStore) != null) {
            log.info("Endereço criado: {}", addressStore.getId());
        }
    }

    public Set<AddressStore> getAddressByStoreEmail(String email) {
        Set<AddressStore> addresses =
                this.addressStoreRepository.findAddressesByStore(storeService.getStoreByEmail(email))
                .orElseThrow(() -> new NotFoundException("Endereço não encontrado"));

        if (addresses.isEmpty()) {
            throw new NotFoundException("Endereço não encontrado");
        }

        return addresses;
    }

    public AddressStore getAddressById(BigInteger id) {
        return this.addressStoreRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Endereço não encontrado"));
    }

    @Transactional
    public void putAddress(BigInteger id, AddressStore addressStore) {
        AddressStore address1 = addressStoreRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Endereço não encontrado"));

        address1.setStreet(addressStore.getStreet());
        address1.setNumber(addressStore.getNumber());
        address1.setDistrict(addressStore.getDistrict());
        address1.setCity(addressStore.getCity());
        address1.setCep(addressStore.getCep());
        address1.setState(addressStore.getState());

        Set<ConstraintViolation<AddressStore>> validate = this.validator.validate(address1);

        if (!validate.isEmpty()) {
            throw new ConstraintViolationException("Endereço inválido!", validate);
        }

        if (addressStoreRepository.save(address1) != null) {
            log.info("Endereço alterado: {}", address1.getId());
        }
    }

    @Transactional
    public void deleteAddress(BigInteger id) {
        AddressStore address = addressStoreRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Endereço não encontrado"));
        addressStoreRepository.delete(address);
        log.info("Endereço deletetado: {}", address.getId());
    }
}
