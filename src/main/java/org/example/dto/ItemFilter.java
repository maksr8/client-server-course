package org.example.dto;

public record ItemFilter(
    String name,
    String category,
    Double minPrice,
    Double maxPrice,
    Integer minQuantity,
    Integer maxQuantity,
    int limit,
    int offset
) {}