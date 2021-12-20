package com.netbull.apiloja.service;

import com.netbull.apiloja.domain.address.addressStore.AddressStoreRepository;
import com.netbull.apiloja.domain.address.addressStore.AddressStore;
import com.netbull.apiloja.domain.store.Store;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.*;
import javax.ws.rs.NotFoundException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AddressServiceTest {

    private AddressService addressService;
    private Validator validation;
    private AddressStoreRepository addressStoreRepository;
    private StoreService storeService;

    @BeforeAll
    public void antesDeTodosTestes() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        this.validation = validatorFactory.getValidator();

    }

    @BeforeEach
    public void inicioCadaTeste() {
        this.addressStoreRepository = Mockito.mock(AddressStoreRepository.class);
        this.storeService = Mockito.mock(StoreService.class);
        this.addressService = new AddressService(validation, addressStoreRepository
                , storeService);
    }

    @Test
    @DisplayName("Testa quando o endereço for nulo")
    public void testa_quando_EnderecoNull_lancaExcecao() {
        assertNotNull(addressService);

        AddressStore address = null;

        when(storeService.getStoreByEmail(any())).thenReturn(new Store());

        var assertThrows = assertThrows(NullPointerException.class,
                () -> addressService.persistAddress(address, "email"));

        assertNotNull(assertThrows);
    }

    @Test
    @DisplayName("Testa todos os atributos do endereço quando forem nulos")
    public void testa_quando_atributosDoEnderecoEhNull_lancaException() {
        assertNotNull(addressService);

        AddressStore address = new AddressStore();

        var assertThrows = assertThrows(ConstraintViolationException.class,
                () -> addressService.persistAddress(address, "email"));

        assertEquals(7, assertThrows.getConstraintViolations().size());

        List<String> messages = assertThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "A loja não pode ser nula.",
                "A rua não pode ser vazia.",
                "O número não pode ser vazio.",
                "O bairro não pode ser vazio.",
                "A cidade não pode ser vazia.",
                "O CEP não pode ser vazio.",
                "O Estado não pode ser vazio."
        ));
    }

    @Test
    @DisplayName("Testa todos os atributos do endereço quando não estão no padrão")
    public void testa_quando_atributosEstaoForaDoPadrao_lancaException() {
        assertNotNull(addressService);

        AddressStore address = new AddressStore();

        address.setStreet(null);
        address.setNumber("aaa");
        address.setDistrict(null);
        address.setCity(null);
        address.setCep("aaaaaaaaa");
        address.setState(null);

        var assertThrows1 = assertThrows(ConstraintViolationException.class,
                () -> addressService.persistAddress(address, ""));
        assertEquals(7, assertThrows1.getConstraintViolations().size());

        List<String> messages = assertThrows1.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "A loja não pode ser nula.",
                "A rua não pode ser vazia.",
                "Número inválido.",
                "O bairro não pode ser vazio.",
                "A cidade não pode ser vazia.",
                "CEP inválido.",
                "O Estado não pode ser vazio."
        ));

    }

    @Test
    @DisplayName("Testa todos os atributos do endereço quando estão no padrão")
    public void testa_quando_atributosEstaoDentroDoPadrao() {
        assertNotNull(addressService);

        AddressStore address = new AddressStore();

        address.setStore(new Store());
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");

        when(storeService.getStoreByEmail(any())).thenReturn(new Store());

        addressService.persistAddress(address, "email");

        then(addressStoreRepository).should(times(1)).save(any());
    }

    @Test
    @DisplayName("Testa busca de endereço pelo email do cliente quando id não existe")
    public void testa_buscaDeEndereçoPeloIdClienteNaoExiste_lancaException() {
        assertNotNull(this.addressService);

        var assertThrows = assertThrows(NotFoundException.class,
                () -> addressService.getAddressByStoreEmail(""));

        assertEquals("Endereço não encontrado", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa busca de endereço pelo email do cliente quando id existe")
    public void testa_buscaDeEndereçoPeloIdClienteExiste() {
        assertNotNull(this.addressService);

        Set<AddressStore> addresses = new HashSet<>();
        for (int x = 0; x < 10; x++) {
            AddressStore address = new AddressStore();
            address.setId(BigInteger.valueOf(x));
            addresses.add(address);
        }
        when(addressStoreRepository.findAddressesByStore(any())).thenReturn(Optional.of(addresses));

        Set<AddressStore> addressesResponse = addressService.getAddressByStoreEmail("");

        then(addressStoreRepository).should(times(1)).findAddressesByStore(any());

        assertEquals(10, addressesResponse.size());
    }

    @Test
    @DisplayName("Testa alteração de endereço quando ele existe e quando ele não existe")
    public void testa_alteracaoDoEndereco() {
        assertNotNull(this.addressService);

        var assertThrows1 = assertThrows(NotFoundException.class,
                () -> addressService.putAddress(BigInteger.ONE, new AddressStore()));

        assertEquals("Endereço não encontrado", assertThrows1.getMessage());

        when(addressStoreRepository.findById(BigInteger.ONE)).thenReturn(Optional.of(new AddressStore()));

        var assertThrows2 = assertThrows(ConstraintViolationException.class,
                () -> addressService.putAddress(BigInteger.ONE, new AddressStore()));

        assertEquals(7, assertThrows2.getConstraintViolations().size());

        List<String> messages = assertThrows2.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "A loja não pode ser nula.",
                "A rua não pode ser vazia.",
                "O número não pode ser vazio.",
                "O bairro não pode ser vazio.",
                "A cidade não pode ser vazia.",
                "O CEP não pode ser vazio.",
                "O Estado não pode ser vazio."
        ));

        AddressStore address = new AddressStore();

        address.setId(BigInteger.TEN);
        address.setStore(new Store());
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");

        AddressStore newAddress = new AddressStore();
        newAddress.setStreet("Avenida");
        newAddress.setNumber("123456");
        newAddress.setDistrict("Centro");
        newAddress.setCity("Feliz");
        newAddress.setCep("95773000");
        newAddress.setState("Rio Grande do Sul");

        when(addressStoreRepository.findById(address.getId())).thenReturn(Optional.of(address));

        addressService.putAddress(address.getId(), newAddress);

        assertEquals(address.getCity(), newAddress.getCity());
        assertEquals(address.getStreet(), newAddress.getStreet());

        then(addressStoreRepository).should(times(2)).findById(BigInteger.ONE);
        then(addressStoreRepository).should(times(1)).findById(BigInteger.TEN);
        then(addressStoreRepository).should(times(1)).save(address);
    }

    @Test
    @DisplayName("Testa o método delete")
    public void testa_metodoDelete() {
        assertNotNull(addressService);

        var assertThrow = assertThrows(NotFoundException.class,
                () -> addressService.deleteAddress(BigInteger.ONE));

        assertEquals("Endereço não encontrado", assertThrow.getMessage());

        AddressStore address = new AddressStore();

        address.setId(BigInteger.TEN);
        address.setStore(new Store());
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");

        when(storeService.getStoreByEmail(any())).thenReturn(new Store());

        addressService.persistAddress(address, "email");

        then(addressStoreRepository).should(times(1)).save(address);

        when(addressStoreRepository.findById(any())).thenReturn(Optional.of(address));

        addressService.deleteAddress(BigInteger.ONE);

        then(addressStoreRepository).should(times(1)).delete(address);
    }
}