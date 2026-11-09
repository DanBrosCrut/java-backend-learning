package com.example.moysklad.api.service;

import com.example.moysklad.api.config.CartProperties;
import com.example.moysklad.api.dto.CartItemRequest;
import com.example.moysklad.api.dto.CartItemView;
import com.example.moysklad.api.dto.CartView;
import com.example.moysklad.api.exception.CartItemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CartService}: add, increment, remove and total quantity.
 */
class CartServiceTest {

    private static final String PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SECOND_PRODUCT_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    @TempDir
    private Path tempDir;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        CartStore store = new CartStore(
                new CartProperties(tempDir.resolve("cart.json").toString()),
                new ObjectMapper());
        cartService = new CartService(store);
    }

    @Test
    void shouldReturnEmptyCart_whenNothingWasAdded() {
        CartView cart = cartService.getCart();

        assertThat(cart.items()).isEmpty();
        assertThat(cart.totalQuantity()).isZero();
    }

    @Test
    void shouldAddNewItem_whenProductIsNotInCart() {
        CartItemView item = cartService.addItem(new CartItemRequest(PRODUCT_ID, 2));

        assertThat(item).isEqualTo(new CartItemView(PRODUCT_ID, 2));
        assertThat(cartService.getCart().items()).containsExactly(item);
    }

    @Test
    void shouldIncreaseQuantity_whenSameProductIsAddedAgain() {
        cartService.addItem(new CartItemRequest(PRODUCT_ID, 2));

        CartItemView updated = cartService.addItem(new CartItemRequest(PRODUCT_ID, 3));

        assertThat(updated.quantity()).isEqualTo(5);
        assertThat(cartService.getCart().items()).containsExactly(new CartItemView(PRODUCT_ID, 5));
    }

    @Test
    void shouldKeepInsertionOrder_whenExistingProductIsAddedAgain() {
        cartService.addItem(new CartItemRequest(PRODUCT_ID, 1));
        cartService.addItem(new CartItemRequest(SECOND_PRODUCT_ID, 1));

        cartService.addItem(new CartItemRequest(PRODUCT_ID, 2));

        assertThat(cartService.getCart().items()).containsExactly(
                new CartItemView(PRODUCT_ID, 3),
                new CartItemView(SECOND_PRODUCT_ID, 1));
    }

    @Test
    void shouldComputeTotalQuantity_whenCartHasSeveralItems() {
        cartService.addItem(new CartItemRequest(PRODUCT_ID, 2));
        cartService.addItem(new CartItemRequest(SECOND_PRODUCT_ID, 3));

        CartView cart = cartService.getCart();

        assertThat(cart.items()).containsExactly(
                new CartItemView(PRODUCT_ID, 2),
                new CartItemView(SECOND_PRODUCT_ID, 3));
        assertThat(cart.totalQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRemoveItem_whenProductIsInCart() {
        cartService.addItem(new CartItemRequest(PRODUCT_ID, 2));
        cartService.addItem(new CartItemRequest(SECOND_PRODUCT_ID, 1));

        cartService.removeItem(PRODUCT_ID);

        assertThat(cartService.getCart().items())
                .containsExactly(new CartItemView(SECOND_PRODUCT_ID, 1));
    }

    @Test
    void shouldThrowCartItemNotFoundException_whenProductIsNotInCart() {
        assertThatThrownBy(() -> cartService.removeItem(PRODUCT_ID))
                .isInstanceOf(CartItemNotFoundException.class)
                .hasMessageContaining(PRODUCT_ID);
    }
}
