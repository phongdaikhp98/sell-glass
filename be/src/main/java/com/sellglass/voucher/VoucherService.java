package com.sellglass.voucher;

import com.sellglass.common.response.PageResponse;
import com.sellglass.voucher.dto.ApplyVoucherResponse;
import com.sellglass.voucher.dto.VoucherRequest;
import com.sellglass.voucher.dto.VoucherResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface VoucherService {

    PageResponse<VoucherResponse> findAll(String search, Pageable pageable);

    VoucherResponse create(VoucherRequest request);

    VoucherResponse update(UUID id, VoucherRequest request);

    void delete(UUID id);

    ApplyVoucherResponse apply(String code, BigDecimal orderTotal);

    void incrementUsage(String code);
}
