package com.sellglass.customer;

import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.common.response.PageResponse;
import com.sellglass.customer.dto.CustomerAddressRequest;
import com.sellglass.customer.dto.CustomerAddressResponse;
import com.sellglass.customer.dto.CustomerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;

    @Override
    public PageResponse<CustomerResponse> findAll(Pageable pageable) {
        return PageResponse.of(customerRepository.findAll(pageable).map(CustomerResponse::from));
    }

    @Override
    public CustomerResponse findById(UUID id) {
        return CustomerResponse.from(customerRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Customer not found")));
    }

    @Override
    public CustomerResponse getProfile(UUID customerId) {
        return findById(customerId);
    }

    @Override
    public List<CustomerAddressResponse> getAddresses(UUID customerId) {
        return customerAddressRepository.findByCustomerId(customerId).stream()
                .map(CustomerAddressResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public CustomerAddressResponse addAddress(UUID customerId, CustomerAddressRequest request) {
        if (request.isDefault()) {
            customerAddressRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                    .ifPresent(existing -> {
                        existing.setDefault(false);
                        customerAddressRepository.save(existing);
                    });
        }
        CustomerAddress address = new CustomerAddress();
        address.setCustomerId(customerId);
        address.setReceiverName(request.getReceiverName());
        address.setPhone(request.getPhone());
        address.setAddress(request.getAddress());
        address.setDefault(request.isDefault());
        return CustomerAddressResponse.from(customerAddressRepository.save(address));
    }

    @Override
    @Transactional
    public CustomerAddressResponse updateAddress(UUID customerId, UUID addressId, CustomerAddressRequest request) {
        CustomerAddress address = customerAddressRepository.findById(addressId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Address not found"));
        if (!address.getCustomerId().equals(customerId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Address does not belong to this customer");
        }
        address.setReceiverName(request.getReceiverName());
        address.setPhone(request.getPhone());
        address.setAddress(request.getAddress());
        address.setDefault(request.isDefault());
        return CustomerAddressResponse.from(customerAddressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = customerAddressRepository.findById(addressId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Address not found"));
        if (!address.getCustomerId().equals(customerId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Address does not belong to this customer");
        }
        customerAddressRepository.delete(address);
    }
}
