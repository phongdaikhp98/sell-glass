package com.sellglass.catalog.brand;

import com.sellglass.catalog.brand.dto.BrandRequest;
import com.sellglass.catalog.brand.dto.BrandResponse;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    @Override
    public List<BrandResponse> findAll() {
        return brandRepository.findAll().stream().map(BrandResponse::from).toList();
    }

    @Override
    public BrandResponse findById(UUID id) {
        return BrandResponse.from(brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Brand not found")));
    }

    @Override
    @Transactional
    public BrandResponse create(BrandRequest request) {
        if (brandRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.CONFLICT, "Slug already in use");
        }
        Brand brand = new Brand();
        brand.setName(request.getName());
        brand.setSlug(request.getSlug());
        brand.setLogoUrl(request.getLogoUrl());
        brand.setActive(request.isActive());
        return BrandResponse.from(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public BrandResponse update(UUID id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Brand not found"));
        if (!brand.getSlug().equals(request.getSlug()) && brandRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.CONFLICT, "Slug already in use");
        }
        brand.setName(request.getName());
        brand.setSlug(request.getSlug());
        brand.setLogoUrl(request.getLogoUrl());
        brand.setActive(request.isActive());
        return BrandResponse.from(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!brandRepository.existsById(id)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Brand not found");
        }
        brandRepository.deleteById(id);
    }
}
