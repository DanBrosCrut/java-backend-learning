package com.example.moysklad.api.service;

import com.example.moysklad.api.dto.CartItemRequest;
import com.example.moysklad.api.dto.CartItemView;
import com.example.moysklad.api.dto.CartView;
import com.example.moysklad.api.exception.CartItemNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Business logic for the file-based cart.
 * <p>
 * Positions are identified by product ID. Adding a product that is already in the cart increases its
 * quantity instead of creating a duplicate position. Mutations are serialized so concurrent requests
 * cannot lose an update between reading and writing the cart file.
 */
@Service
public class CartService {

    private final CartStore cartStore;
    private final Object mutationLock = new Object();

    /**
     * Creates the service with the cart store.
     *
     * @param cartStore the file-backed cart store
     */
    public CartService(CartStore cartStore) {
        this.cartStore = cartStore;
    }

    /**
     * Returns the complete cart with the computed total quantity.
     *
     * @return the cart contents
     */
    public CartView getCart() {
        return CartView.of(cartStore.load());
    }

    /**
     * Adds a position to the cart, increasing the quantity when the product is already present.
     *
     * @param request the product and quantity to add
     * @return the resulting cart position
     */
    public CartItemView addItem(CartItemRequest request) {
        synchronized (mutationLock) {
            List<CartItemView> items = new ArrayList<>(cartStore.load());
            int existingIndex = IntStream.range(0, items.size())
                    .filter(index -> items.get(index).productId().equals(request.productId()))
                    .findFirst()
                    .orElse(-1);
            CartItemView updated = existingIndex < 0
                    ? new CartItemView(request.productId(), request.quantity())
                    : new CartItemView(request.productId(),
                            items.get(existingIndex).quantity() + request.quantity());
            if (existingIndex < 0) {
                items.add(updated);
            } else {
                items.set(existingIndex, updated);
            }
            cartStore.save(items);
            return updated;
        }
    }

    /**
     * Removes a position from the cart.
     *
     * @param productId the product ID to remove
     * @throws CartItemNotFoundException when the product is not in the cart
     */
    public void removeItem(String productId) {
        synchronized (mutationLock) {
            List<CartItemView> items = cartStore.load();
            List<CartItemView> remaining = items.stream()
                    .filter(item -> !item.productId().equals(productId))
                    .toList();
            if (remaining.size() == items.size()) {
                throw new CartItemNotFoundException(productId);
            }
            cartStore.save(remaining);
        }
    }
}
