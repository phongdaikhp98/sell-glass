package com.sellglass.customer.dto;

import com.sellglass.customer.Customer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CustomerResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private LocalDateTime createdAt;

    public static CustomerResponse from(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.id = customer.getId();
        response.fullName = customer.getFullName();
        response.email = customer.getEmail();
        response.phone = customer.getPhone();
        response.createdAt = customer.getCreatedAt();
        return response;
    }
}
