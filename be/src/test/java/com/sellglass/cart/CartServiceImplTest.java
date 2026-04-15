package com.sellglass.cart;

import com.sellglass.cart.dto.CartItemRequest;
import com.sellglass.cart.dto.CartResponse;
import com.sellglass.catalog.product.Product;
import com.sellglass.catalog.product.ProductImage;
import com.sellglass.catalog.product.ProductImageRepository;
import com.sellglass.catalog.product.ProductRepository;
import com.sellglass.catalog.variant.ProductVariant;
import com.sellglass.catalog.variant.ProductVariantRepository;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;

    @InjectMocks
    private CartServiceImpl service;

    private UUID customerId;
    private UUID cartId;
    private UUID variantId;
    private UUID productId;
    private UUID cartItemId;

    private Cart cart;
    private CartItem cartItem;
    private ProductVariant variant;
    private Product product;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        cartId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        cartItemId = UUID.randomUUID();

        cart = new Cart();
        cart.setId(cartId);
        cart.setCustomerId(customerId);

        variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProductId(productId);
        variant.setSku("SKU-001");
        variant.setColor("Black");
        variant.setSize("M");
        variant.setPrice(new BigDecimal("500000"));

        product = new Product();
        product.setId(productId);
        product.setName("Kính A");

        cartItem = new CartItem();
        cartItem.setId(cartItemId);
        cartItem.setCartId(cartId);
        cartItem.setProductVariantId(variantId);
        cartItem.setQuantity(2);
    }

    private void mockBuildCartResponse(List<CartItem> items) {
        when(cartItemRepository.findByCartId(cartId)).thenReturn(items);
        if (!items.isEmpty()) {
            when(productVariantRepository.findAllById(any())).thenReturn(List.of(variant));
            when(productRepository.findAllById(any())).thenReturn(List.of(product));
            when(productImageRepository.findByProductIdIn(any())).thenReturn(List.of());
        } else {
            when(productVariantRepository.findAllById(any())).thenReturn(List.of());
            when(productRepository.findAllById(any())).thenReturn(List.of());
            when(productImageRepository.findByProductIdIn(any())).thenReturn(List.of());
        }
    }

    @Test
    @DisplayName("getCart should create cart when customer has none")
    void getCart_createsCartIfAbsent() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        mockBuildCartResponse(List.of());

        CartResponse result = service.getCart(customerId);

        assertThat(result.getCartId()).isEqualTo(cartId);
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("getCart should return existing cart with enriched items")
    void getCart_existingCart() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        mockBuildCartResponse(List.of(cartItem));

        CartResponse result = service.getCart(customerId);

        assertThat(result.getCartId()).isEqualTo(cartId);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("Kính A");
        assertThat(result.getItems().get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("1000000")); // 500k*2
    }

    @Test
    @DisplayName("addItem should create new CartItem when variant not yet in cart")
    void addItem_newItem() {
        CartItemRequest request = new CartItemRequest();
        request.setProductVariantId(variantId);
        request.setQuantity(1);

        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variantId)).thenReturn(Optional.empty());
        mockBuildCartResponse(List.of(cartItem));

        service.addItem(customerId, request);

        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartItemRepository).flush();
    }

    @Test
    @DisplayName("addItem should increment quantity when variant already in cart")
    void addItem_incrementsQuantity() {
        CartItemRequest request = new CartItemRequest();
        request.setProductVariantId(variantId);
        request.setQuantity(3);

        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variantId))
                .thenReturn(Optional.of(cartItem));
        mockBuildCartResponse(List.of(cartItem));

        service.addItem(customerId, request);

        assertThat(cartItem.getQuantity()).isEqualTo(5); // 2 + 3
        verify(cartItemRepository, never()).save(any(CartItem.class)); // no new save for existing
        verify(cartItemRepository).flush();
    }

    @Test
    @DisplayName("updateItem should set new quantity")
    void updateItem_success() {
        CartItemRequest request = new CartItemRequest();
        request.setProductVariantId(variantId);
        request.setQuantity(5);

        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(cartItem)).thenReturn(cartItem);
        mockBuildCartResponse(List.of(cartItem));

        service.updateItem(customerId, cartItemId, request);

        assertThat(cartItem.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(cartItem);
    }

    @Test
    @DisplayName("updateItem should throw NOT_FOUND when item missing")
    void updateItem_notFound() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateItem(customerId, cartItemId, new CartItemRequest()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("updateItem should throw FORBIDDEN when item belongs to different cart")
    void updateItem_wrongCart() {
        UUID otherCartId = UUID.randomUUID();
        cartItem.setCartId(otherCartId);

        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));

        assertThatThrownBy(() -> service.updateItem(customerId, cartItemId, new CartItemRequest()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("removeItem should delete item from cart")
    void removeItem_success() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));

        service.removeItem(customerId, cartItemId);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    @DisplayName("removeItem should throw FORBIDDEN when item belongs to different cart")
    void removeItem_wrongCart() {
        cartItem.setCartId(UUID.randomUUID());
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));

        assertThatThrownBy(() -> service.removeItem(customerId, cartItemId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    @DisplayName("clearCart should delete all items when cart exists")
    void clearCart_success() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));

        service.clearCart(customerId);

        verify(cartItemRepository).deleteByCartId(cartId);
    }

    @Test
    @DisplayName("clearCart should be no-op when cart not found")
    void clearCart_noCart() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        service.clearCart(customerId);

        verify(cartItemRepository, never()).deleteByCartId(any());
    }

    @Test
    @DisplayName("getCart should attach primary image when available")
    void getCart_withPrimaryImage() {
        ProductImage image = new ProductImage();
        image.setProductId(productId);
        image.setUrl("https://cdn.example.com/img.jpg");
        image.setPrimary(true);

        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cartId)).thenReturn(List.of(cartItem));
        when(productVariantRepository.findAllById(any())).thenReturn(List.of(variant));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(productImageRepository.findByProductIdIn(any())).thenReturn(List.of(image));

        CartResponse result = service.getCart(customerId);

        assertThat(result.getItems().get(0).getPrimaryImageUrl())
                .isEqualTo("https://cdn.example.com/img.jpg");
    }
}
