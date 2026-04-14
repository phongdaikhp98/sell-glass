package com.sellglass.cart;

import com.sellglass.cart.dto.CartItemRequest;
import com.sellglass.cart.dto.CartResponse;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    public CartResponse getCart(UUID customerId) {
        Cart cart = getOrCreateCart(customerId);
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(UUID customerId, CartItemRequest request) {
        Cart cart = getOrCreateCart(customerId);

        cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), request.getProductVariantId())
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + request.getQuantity()),
                        () -> {
                            CartItem item = new CartItem();
                            item.setCartId(cart.getId());
                            item.setProductVariantId(request.getProductVariantId());
                            item.setQuantity(request.getQuantity());
                            cartItemRepository.save(item);
                        }
                );

        // TODO: Flush context to reflect updated item in response
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(UUID customerId, UUID itemId, CartItemRequest request) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Cart item not found"));
        if (!item.getCartId().equals(cart.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Item does not belong to this cart");
        }
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public void removeItem(UUID customerId, UUID itemId) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Cart item not found"));
        if (!item.getCartId().equals(cart.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Item does not belong to this cart");
        }
        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clearCart(UUID customerId) {
        cartRepository.findByCustomerId(customerId)
                .ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getId()));
    }

    private Cart getOrCreateCart(UUID customerId) {
        return cartRepository.findByCustomerId(customerId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setCustomerId(customerId);
            return cartRepository.save(cart);
        });
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartResponse.CartItemDetail> details = items.stream()
                .map(i -> new CartResponse.CartItemDetail(i.getId(), i.getProductVariantId(), i.getQuantity()))
                .toList();
        return new CartResponse(cart.getId(), details);
    }
}
