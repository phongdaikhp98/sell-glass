package com.sellglass.customer.dto;

import com.sellglass.customer.CustomerAddress;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CustomerAddressResponse {

    private UUID id;
    private UUID customerId;
    private String receiverName;
    private String phone;
    private String address;
    private boolean isDefault;

    public static CustomerAddressResponse from(CustomerAddress address) {
        CustomerAddressResponse response = new CustomerAddressResponse();
        response.id = address.getId();
        response.customerId = address.getCustomerId();
        response.receiverName = address.getReceiverName();
        response.phone = address.getPhone();
        response.address = address.getAddress();
        response.isDefault = address.isDefault();
        return response;
    }
}
