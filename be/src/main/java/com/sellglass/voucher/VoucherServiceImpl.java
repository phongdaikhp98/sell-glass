package com.sellglass.voucher;

import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.common.response.PageResponse;
import com.sellglass.voucher.dto.ApplyVoucherResponse;
import com.sellglass.voucher.dto.VoucherRequest;
import com.sellglass.voucher.dto.VoucherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    public PageResponse<VoucherResponse> findAll(String search, Pageable pageable) {
        return PageResponse.of(
                voucherRepository.search(search, pageable).map(VoucherResponse::from)
        );
    }

    @Override
    @Transactional
    public VoucherResponse create(VoucherRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (voucherRepository.existsByCodeIgnoreCase(code)) {
            throw new AppException(ErrorCode.CONFLICT, "Mã voucher đã tồn tại");
        }
        Voucher voucher = new Voucher();
        mapRequest(voucher, request, code);
        return VoucherResponse.from(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public VoucherResponse update(UUID id, VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Voucher not found"));
        String code = request.getCode().trim().toUpperCase();
        if (!voucher.getCode().equalsIgnoreCase(code) && voucherRepository.existsByCodeIgnoreCase(code)) {
            throw new AppException(ErrorCode.CONFLICT, "Mã voucher đã tồn tại");
        }
        mapRequest(voucher, request, code);
        return VoucherResponse.from(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!voucherRepository.existsById(id)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Voucher not found");
        }
        voucherRepository.deleteById(id);
    }

    @Override
    public ApplyVoucherResponse apply(String code, BigDecimal orderTotal) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST, "Mã giảm giá không hợp lệ"));

        if (!voucher.isActive()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã giảm giá không còn hiệu lực");
        }
        if (voucher.getExpiresAt() != null && voucher.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã giảm giá đã hết hạn");
        }
        if (voucher.getUsageLimit() != null && voucher.getTimesUsed() >= voucher.getUsageLimit()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã giảm giá đã hết lượt sử dụng");
        }
        if (orderTotal.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Đơn hàng tối thiểu " + formatVnd(voucher.getMinOrderAmount()) + " để dùng mã này");
        }

        BigDecimal discount = calculateDiscount(voucher, orderTotal);
        String description = buildDescription(voucher);

        return new ApplyVoucherResponse(discount, description);
    }

    @Override
    @Transactional
    public void incrementUsage(String code) {
        voucherRepository.findByCodeIgnoreCase(code).ifPresent(v -> {
            v.setTimesUsed(v.getTimesUsed() + 1);
            voucherRepository.save(v);
        });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void mapRequest(Voucher voucher, VoucherRequest request, String code) {
        voucher.setCode(code);
        voucher.setType(request.getType());
        voucher.setValue(request.getValue());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setMinOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO);
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setExpiresAt(request.getExpiresAt());
        voucher.setActive(request.isActive());
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderTotal) {
        if (voucher.getType() == VoucherType.PERCENTAGE) {
            BigDecimal discount = orderTotal
                    .multiply(voucher.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null) {
                discount = discount.min(voucher.getMaxDiscountAmount());
            }
            return discount;
        } else {
            return voucher.getValue().min(orderTotal);
        }
    }

    private String buildDescription(Voucher voucher) {
        if (voucher.getType() == VoucherType.PERCENTAGE) {
            String desc = "Giảm " + voucher.getValue().stripTrailingZeros().toPlainString() + "%";
            if (voucher.getMaxDiscountAmount() != null) {
                desc += " (tối đa " + formatVnd(voucher.getMaxDiscountAmount()) + ")";
            }
            return desc;
        } else {
            return "Giảm " + formatVnd(voucher.getValue());
        }
    }

    private String formatVnd(BigDecimal amount) {
        return String.format("%,.0f đ", amount);
    }
}
