package com.example.moysklad.api.service;

import com.example.moysklad.api.config.CartProperties;
import com.example.moysklad.api.dto.CartItemView;
import com.example.moysklad.api.exception.CartStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CartStore}: JSON persistence, missing files and storage failures.
 */
class CartStoreTest {

    private static final String PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SECOND_PRODUCT_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    @TempDir
    private Path tempDir;

    @Test
    void shouldReturnEmptyList_whenCartFileDoesNotExist() {
        CartStore store = storeAt(tempDir.resolve("missing").resolve("cart.json"));

        assertThat(store.load()).isEmpty();
    }

    @Test
    void shouldPersistItemsAcrossStoreInstances_whenCartIsSaved() {
        Path cartFile = tempDir.resolve("cart.json");
        CartStore first = storeAt(cartFile);
        List<CartItemView> items = List.of(
                new CartItemView(PRODUCT_ID, 2),
                new CartItemView(SECOND_PRODUCT_ID, 3));

        first.save(items);

        CartStore second = storeAt(cartFile);
        assertThat(second.load()).containsExactlyElementsOf(items);
        assertThat(cartFile).exists();
    }

    @Test
    void shouldCreateParentDirectories_whenSavingCart() {
        Path cartFile = tempDir.resolve("nested").resolve("data").resolve("cart.json");
        CartStore store = storeAt(cartFile);

        store.save(List.of(new CartItemView(PRODUCT_ID, 1)));

        assertThat(cartFile).exists();
        assertThat(store.load()).containsExactly(new CartItemView(PRODUCT_ID, 1));
    }

    @Test
    void shouldThrowCartStorageException_whenCartFileIsMalformed() throws Exception {
        Path cartFile = tempDir.resolve("cart.json");
        Files.writeString(cartFile, "not-json");
        CartStore store = storeAt(cartFile);

        assertThatThrownBy(store::load)
                .isInstanceOf(CartStorageException.class)
                .hasMessageContaining("Failed to read cart file");
    }

    private static CartStore storeAt(Path cartFile) {
        return new CartStore(new CartProperties(cartFile.toString()), new ObjectMapper());
    }
}
