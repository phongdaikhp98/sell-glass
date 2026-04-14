package com.sellglass.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CartItemRequest {

    @NotNull(message = "Product variant ID is required")
    private UUID productVariantId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;
}
