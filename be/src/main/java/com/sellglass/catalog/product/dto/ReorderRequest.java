package com.sellglass.catalog.product.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ReorderRequest {

    @NotNull
    private UUID id;

    private int sortOrder;
}
