package com.sellglass.customer;

import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.customer.dto.CustomerAddressRequest;
import com.sellglass.customer.dto.CustomerAddressResponse;
import com.sellglass.customer.dto.CustomerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerAddressRepository customerAddressRepository;

    @InjectMocks
    private CustomerServiceImpl service;

    private UUID customerId;
    private UUID addressId;
    private Customer customer;
    private CustomerAddress address;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        customer = new Customer();
        customer.setId(customerId);
        customer.setFullName("Nguyen Van A");
        customer.setEmail("a@example.com");
        customer.setPhone("0901234567");

        address = new CustomerAddress();
        address.setId(addressId);
        address.setCustomerId(customerId);
        address.setReceiverName("Receiver");
        address.setPhone("0909999999");
        address.setAddress("123 Street");
        address.setDefault(false);
    }

    @Test
    @DisplayName("findById should return customer when exists")
    void findById_success() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        CustomerResponse result = service.findById(customerId);

        assertThat(result.getId()).isEqualTo(customerId);
        assertThat(result.getEmail()).isEqualTo("a@example.com");
    }

    @Test
    @DisplayName("findById should throw NOT_FOUND when customer missing")
    void findById_notFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(customerId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("getAddresses should return all addresses for customer")
    void getAddresses_success() {
        when(customerAddressRepository.findByCustomerId(customerId)).thenReturn(List.of(address));

        List<CustomerAddressResponse> result = service.getAddresses(customerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReceiverName()).isEqualTo("Receiver");
    }

    @Test
    @DisplayName("addAddress should save new address without touching default flag when not default")
    void addAddress_nonDefault() {
        CustomerAddressRequest request = new CustomerAddressRequest();
        request.setReceiverName("Receiver");
        request.setPhone("0909999999");
        request.setAddress("123 Street");
        request.setDefault(false);

        when(customerAddressRepository.save(any(CustomerAddress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CustomerAddressResponse result = service.addAddress(customerId, request);

        assertThat(result.getReceiverName()).isEqualTo("Receiver");
        verify(customerAddressRepository, never()).findByCustomerIdAndIsDefaultTrue(any());
    }

    @Test
    @DisplayName("addAddress should unset existing default when adding new default")
    void addAddress_unsetPreviousDefault() {
        CustomerAddress existingDefault = new CustomerAddress();
        existingDefault.setId(UUID.randomUUID());
        existingDefault.setCustomerId(customerId);
        existingDefault.setDefault(true);

        CustomerAddressRequest request = new CustomerAddressRequest();
        request.setReceiverName("New");
        request.setPhone("0901111111");
        request.setAddress("999 Street");
        request.setDefault(true);

        when(customerAddressRepository.findByCustomerIdAndIsDefaultTrue(customerId))
                .thenReturn(Optional.of(existingDefault));
        when(customerAddressRepository.save(any(CustomerAddress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.addAddress(customerId, request);

        ArgumentCaptor<CustomerAddress> captor = ArgumentCaptor.forClass(CustomerAddress.class);
        verify(customerAddressRepository, times(2)).save(captor.capture());
        List<CustomerAddress> saved = captor.getAllValues();
        assertThat(saved.get(0).isDefault()).isFalse();
        assertThat(saved.get(1).isDefault()).isTrue();
    }

    @Test
    @DisplayName("updateAddress should update fields for owner")
    void updateAddress_success() {
        CustomerAddressRequest request = new CustomerAddressRequest();
        request.setReceiverName("Updated");
        request.setPhone("0901111111");
        request.setAddress("Updated address");
        request.setDefault(true);

        when(customerAddressRepository.findById(addressId)).thenReturn(Optional.of(address));
        when(customerAddressRepository.save(any(CustomerAddress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CustomerAddressResponse result = service.updateAddress(customerId, addressId, request);

        assertThat(result.getReceiverName()).isEqualTo("Updated");
        assertThat(result.isDefault()).isTrue();
    }

    @Test
    @DisplayName("updateAddress should throw FORBIDDEN when not owner")
    void updateAddress_notOwner() {
        UUID otherCustomer = UUID.randomUUID();
        CustomerAddressRequest request = new CustomerAddressRequest();
        request.setReceiverName("X");
        request.setPhone("0");
        request.setAddress("X");

        when(customerAddressRepository.findById(addressId)).thenReturn(Optional.of(address));

        assertThatThrownBy(() -> service.updateAddress(otherCustomer, addressId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(customerAddressRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateAddress should throw NOT_FOUND when address missing")
    void updateAddress_notFound() {
        when(customerAddressRepository.findById(addressId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAddress(customerId, addressId, new CustomerAddressRequest()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("deleteAddress should remove when owner")
    void deleteAddress_success() {
        when(customerAddressRepository.findById(addressId)).thenReturn(Optional.of(address));

        service.deleteAddress(customerId, addressId);

        verify(customerAddressRepository).delete(address);
    }

    @Test
    @DisplayName("deleteAddress should throw FORBIDDEN when not owner")
    void deleteAddress_notOwner() {
        UUID otherCustomer = UUID.randomUUID();
        when(customerAddressRepository.findById(addressId)).thenReturn(Optional.of(address));

        assertThatThrownBy(() -> service.deleteAddress(otherCustomer, addressId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(customerAddressRepository, never()).delete(any(CustomerAddress.class));
    }
}
