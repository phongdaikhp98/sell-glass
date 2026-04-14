package com.sellglass.cart;

import com.sellglass.cart.dto.CartItemRequest;
import com.sellglass.cart.dto.CartResponse;

import java.util.UUID;

public interface CartService {

    CartResponse getCart(UUID customerId);

    CartResponse addItem(UUID customerId, CartItemRequest request);

    CartResponse updateItem(UUID customerId, UUID itemId, CartItemRequest request);

    void removeItem(UUID customerId, UUID itemId);

    void clearCart(UUID customerId);
}
