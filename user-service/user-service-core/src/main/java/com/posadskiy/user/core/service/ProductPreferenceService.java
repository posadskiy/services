package com.posadskiy.user.core.service;

import com.posadskiy.user.api.ProductPreferenceDto;
import com.posadskiy.user.core.db.ProductPreferenceRepository;
import com.posadskiy.user.core.db.entity.ProductPreferenceEntity;
import com.posadskiy.user.core.db.entity.ProductPreferenceId;
import com.posadskiy.user.core.exception.AuthException;
import jakarta.inject.Singleton;

import java.util.Set;

@Singleton
public class ProductPreferenceService {

    private static final Set<String> PRODUCTS = Set.of("grammarbar", "costy", "repeaty");

    private final ProductPreferenceRepository repository;

    public ProductPreferenceService(ProductPreferenceRepository repository) {
        this.repository = repository;
    }

    private static String product(String product) {
        if (product == null || !PRODUCTS.contains(product)) {
            throw new AuthException("Unknown product");
        }
        return product;
    }

    public ProductPreferenceDto get(Long userId, String product) {
        String normalized = product(product);
        return repository
            .findById(new ProductPreferenceId(userId, normalized))
            .map(row -> new ProductPreferenceDto(normalized, row.marketingEmail(), row.settings()))
            .orElseGet(() -> new ProductPreferenceDto(normalized, false, "{}"));
    }

    public ProductPreferenceDto save(Long userId, String product, boolean marketingEmail, String settings) {
        String normalized = product(product);
        String stored = settings == null || settings.isBlank() ? "{}" : settings;
        if (stored.length() > 4000) {
            throw new AuthException("Settings are too long");
        }
        ProductPreferenceId id = new ProductPreferenceId(userId, normalized);
        ProductPreferenceEntity entity = new ProductPreferenceEntity(id, marketingEmail, stored);
        if (repository.findById(id).isPresent()) {
            repository.update(entity);
        } else {
            repository.save(entity);
        }
        return new ProductPreferenceDto(normalized, marketingEmail, stored);
    }
}
