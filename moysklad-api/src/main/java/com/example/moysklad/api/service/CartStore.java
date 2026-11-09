package com.example.moysklad.api.service;

import com.example.moysklad.api.config.CartProperties;
import com.example.moysklad.api.dto.CartItemView;
import com.example.moysklad.api.exception.CartStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

/**
 * Persists the cart in a JSON file.
 * <p>
 * The whole cart is small, so every read loads the file and every write replaces it atomically
 * through a temporary file in the same directory. The file is the single source of truth, which
 * means the cart survives application restarts without a database.
 */
@Component
public class CartStore {

    private static final Logger log = LoggerFactory.getLogger(CartStore.class);

    private static final TypeReference<List<CartItemView>> CART_TYPE = new TypeReference<>() {
    };

    private final Path file;
    private final ObjectMapper objectMapper;
    private final Object lock = new Object();

    /**
     * Creates the store for the configured cart file.
     *
     * @param properties   the cart configuration
     * @param objectMapper the JSON mapper used to read and write the cart file
     */
    public CartStore(CartProperties properties, ObjectMapper objectMapper) {
        this.file = Path.of(properties.file());
        this.objectMapper = objectMapper;
    }

    /**
     * Loads the cart positions from the file.
     * <p>
     * A missing file means an empty cart.
     *
     * @return the stored cart positions, or an empty list when the file does not exist
     * @throws CartStorageException when the file exists but cannot be read or parsed
     */
    public List<CartItemView> load() {
        synchronized (lock) {
            if (!Files.exists(file)) {
                return List.of();
            }
            try {
                List<CartItemView> items = objectMapper.readValue(file, CART_TYPE);
                return Optional.ofNullable(items).orElseGet(List::of);
            } catch (RuntimeException ex) {
                throw new CartStorageException("Failed to read cart file " + file, ex);
            }
        }
    }

    /**
     * Replaces the cart file with the given positions.
     * <p>
     * The data is first written to {@code cart.json.tmp} and then moved over the cart file with an
     * atomic move when the file system supports it.
     *
     * @param items the complete cart contents to persist
     * @throws CartStorageException when the file cannot be written
     */
    public void save(List<CartItemView> items) {
        synchronized (lock) {
            Path parent = file.toAbsolutePath().getParent();
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try {
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary, items);
                moveAtomically(temporary, file);
                log.debug("Cart saved: {} item(s) to {}", items.size(), file);
            } catch (IOException | RuntimeException ex) {
                throw new CartStorageException("Failed to write cart file " + file, ex);
            } finally {
                deleteQuietly(temporary);
            }
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Could not delete temporary cart file {}: {}", path, ex.getMessage());
        }
    }
}
