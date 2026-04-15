package com.sellglass.cart;

import com.sellglass.cart.dto.CartItemRequest;
import com.sellglass.cart.dto.CartResponse;
import com.sellglass.catalog.product.ProductImage;
import com.sellglass.catalog.product.ProductImageRepository;
import com.sellglass.catalog.product.ProductRepository;
import com.sellglass.catalog.variant.ProductVariant;
import com.sellglass.catalog.variant.ProductVariantRepository;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

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

        cartItemRepository.flush();
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
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        Set<UUID> variantIds = cartItems.stream()
                .map(CartItem::getProductVariantId)
                .collect(Collectors.toSet());

        Map<UUID, ProductVariant> variantMap = productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        Set<UUID> productIds = variantMap.values().stream()
                .map(ProductVariant::getProductId)
                .collect(Collectors.toSet());

        Map<UUID, String> productNameMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(
                        com.sellglass.catalog.product.Product::getId,
                        com.sellglass.catalog.product.Product::getName
                ));

        Map<UUID, String> primaryImageMap = productImageRepository.findByProductIdIn(productIds).stream()
                .filter(ProductImage::isPrimary)
                .collect(Collectors.toMap(
                        ProductImage::getProductId,
                        ProductImage::getUrl,
                        (existing, replacement) -> existing
                ));

        List<CartResponse.CartItemDetail> details = cartItems.stream().map(cartItem -> {
            ProductVariant variant = variantMap.get(cartItem.getProductVariantId());
            String productName = null;
            String primaryImageUrl = null;
            String sku = null;
            String color = null;
            String size = null;
            BigDecimal price = BigDecimal.ZERO;

            if (variant != null) {
                sku = variant.getSku();
                color = variant.getColor();
                size = variant.getSize();
                price = variant.getPrice();
                productName = productNameMap.get(variant.getProductId());
                primaryImageUrl = primaryImageMap.get(variant.getProductId());
            }

            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            return new CartResponse.CartItemDetail(
                    cartItem.getId(),
                    cartItem.getProductVariantId(),
                    productName,
                    primaryImageUrl,
                    sku,
                    color,
                    size,
                    price,
                    cartItem.getQuantity(),
                    subtotal
            );
        }).toList();

        BigDecimal total = details.stream()
                .map(CartResponse.CartItemDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), details, total);
    }
}
