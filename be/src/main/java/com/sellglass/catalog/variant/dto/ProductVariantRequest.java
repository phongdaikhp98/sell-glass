package com.sellglass.catalog.variant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductVariantRequest {

    @NotBlank(message = "SKU is required")
    private String sku;

    private String color;
    private String size;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private boolean isActive = true;

    @Min(value = 0, message = "Stock cannot be negative")
    private int stock = 0;
}
