package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.CartItemRequest;
import com.example.moysklad.api.dto.CartItemView;
import com.example.moysklad.api.dto.CartView;
import com.example.moysklad.api.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the file-based cart.
 */
@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "File-based cart that survives application restarts")
public class CartController {

    private final CartService cartService;

    /**
     * Creates the controller with the cart service.
     *
     * @param cartService the cart business logic
     */
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Returns the complete cart.
     *
     * @return the cart positions and the total quantity
     */
    @Operation(summary = "Get cart", description = "Returns all cart positions and the sum of their quantities")
    @GetMapping
    public CartView getCart() {
        return cartService.getCart();
    }

    /**
     * Adds a position to the cart. Adding a product that is already in the cart increases its quantity.
     *
     * @param request the product and quantity to add
     * @return the resulting cart position
     */
    @Operation(summary = "Add a cart item",
            description = "Adds a product to the cart. Adding the same product again increases its quantity. "
                    + "The cart is persisted to the configured JSON file.")
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemView addItem(@Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(request);
    }

    /**
     * Removes a position from the cart.
     *
     * @param productId the product ID to remove
     */
    @Operation(summary = "Remove a cart item",
            description = "Removes a product from the cart. Returns 404 CART_ITEM_NOT_FOUND "
                    + "when the product is not in the cart.")
    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            @Parameter(description = "Product ID in MoySklad", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String productId) {
        cartService.removeItem(productId);
    }
}
