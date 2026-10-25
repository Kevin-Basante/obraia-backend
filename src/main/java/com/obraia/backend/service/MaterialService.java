package com.obraia.backend.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.catalog.CategoryResponse;
import com.obraia.backend.dto.catalog.MaterialResponse;
import com.obraia.backend.entity.catalog.MaterialCategory;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.MaterialRepository;
import com.obraia.backend.util.TextSearch;

/** Material catalog with reference prices. Read only; filters run on the cached list. */
@Service
public class MaterialService {

    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public List<CategoryResponse> listCategories() {
        return Arrays.stream(MaterialCategory.values()).map(CategoryResponse::from).toList();
    }

    /** Materials sorted by name. The category and the search text are optional. */
    @Transactional(readOnly = true)
    public List<MaterialResponse> listMaterials(String categoryCode, String search) {
        MaterialCategory category = categoryCode == null || categoryCode.isBlank()
                ? null
                : MaterialCategory.fromCode(categoryCode);
        return materialRepository.findAllByOrderByNameAsc().stream()
                .filter(material -> category == null || material.getCategory() == category)
                .filter(material -> TextSearch.matches(material.getName(), search))
                .map(MaterialResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaterialResponse getMaterial(UUID id) {
        return materialRepository.findAllByOrderByNameAsc().stream()
                .filter(material -> material.getId().equals(id))
                .findFirst()
                .map(MaterialResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("MATERIAL_NOT_FOUND",
                        "The material " + id + " does not exist"));
    }
}
