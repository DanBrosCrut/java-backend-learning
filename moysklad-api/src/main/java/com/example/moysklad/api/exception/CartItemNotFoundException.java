package com.example.moysklad.api.exception;

/**
 * Raised when a cart position with the requested product ID does not exist.
 */
public class CartItemNotFoundException extends RuntimeException {

    /**
     * Creates the exception for the missing product.
     *
     * @param productId the product ID that is not in the cart
     */
    public CartItemNotFoundException(String productId) {
        super("Cart item not found: " + productId);
    }
}
