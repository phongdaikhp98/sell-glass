package com.sellglass.catalog.brand;

import com.sellglass.catalog.brand.dto.BrandRequest;
import com.sellglass.catalog.brand.dto.BrandResponse;

import java.util.List;
import java.util.UUID;

public interface BrandService {

    List<BrandResponse> findAll();

    BrandResponse findById(UUID id);

    BrandResponse create(BrandRequest request);

    BrandResponse update(UUID id, BrandRequest request);

    void delete(UUID id);
}
