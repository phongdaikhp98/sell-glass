package com.sellglass.customer;

import com.sellglass.common.response.PageResponse;
import com.sellglass.customer.dto.CustomerAddressRequest;
import com.sellglass.customer.dto.CustomerAddressResponse;
import com.sellglass.customer.dto.CustomerResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    PageResponse<CustomerResponse> findAll(Pageable pageable);

    CustomerResponse findById(UUID id);

    CustomerResponse getProfile(UUID customerId);

    List<CustomerAddressResponse> getAddresses(UUID customerId);

    CustomerAddressResponse addAddress(UUID customerId, CustomerAddressRequest request);

    CustomerAddressResponse updateAddress(UUID customerId, UUID addressId, CustomerAddressRequest request);

    void deleteAddress(UUID customerId, UUID addressId);
}
