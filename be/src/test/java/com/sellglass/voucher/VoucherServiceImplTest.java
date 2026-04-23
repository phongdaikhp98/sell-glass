package com.sellglass.voucher;

import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.voucher.dto.ApplyVoucherResponse;
import com.sellglass.voucher.dto.VoucherRequest;
import com.sellglass.voucher.dto.VoucherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {

    @Mock
    private VoucherRepository voucherRepository;

    @InjectMocks
    private VoucherServiceImpl service;

    private UUID voucherId;
    private Voucher voucher;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        voucherId = UUID.randomUUID();
        pageable = PageRequest.of(0, 10);

        voucher = new Voucher();
        voucher.setId(voucherId);
        voucher.setCode("SAVE10");
        voucher.setType(VoucherType.PERCENTAGE);
        voucher.setValue(new BigDecimal("10"));
        voucher.setMinOrderAmount(BigDecimal.ZERO);
        voucher.setTimesUsed(0);
        voucher.setActive(true);
    }

    // ─── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll should return page of vouchers matching search")
    void findAll_success() {
        Page<Voucher> page = new PageImpl<>(List.of(voucher));
        when(voucherRepository.search(anyString(), eq(pageable))).thenReturn(page);

        var result = service.findAll("SAVE", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCode()).isEqualTo("SAVE10");
    }

    // ─── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create should save voucher when code is unique")
    void create_success() {
        VoucherRequest request = buildRequest("save10", VoucherType.PERCENTAGE, "10");

        when(voucherRepository.existsByCodeIgnoreCase("SAVE10")).thenReturn(false);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> {
            Voucher saved = inv.getArgument(0);
            saved.setId(voucherId);
            return saved;
        });

        VoucherResponse result = service.create(request);

        assertThat(result.getCode()).isEqualTo("SAVE10");
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("create should throw CONFLICT when code already in use")
    void create_codeConflict() {
        VoucherRequest request = buildRequest("SAVE10", VoucherType.PERCENTAGE, "10");

        when(voucherRepository.existsByCodeIgnoreCase("SAVE10")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(voucherRepository, never()).save(any());
    }

    // ─── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update should modify voucher fields")
    void update_success() {
        VoucherRequest request = buildRequest("FLASH50", VoucherType.FIXED_AMOUNT, "50000");

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.existsByCodeIgnoreCase("FLASH50")).thenReturn(false);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> inv.getArgument(0));

        VoucherResponse result = service.update(voucherId, request);

        assertThat(result.getType()).isEqualTo(VoucherType.FIXED_AMOUNT);
        assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("50000"));
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("update should allow same code on the same voucher without conflict")
    void update_sameCode_noConflict() {
        VoucherRequest request = buildRequest("SAVE10", VoucherType.PERCENTAGE, "15");

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(voucherId, request);

        verify(voucherRepository, never()).existsByCodeIgnoreCase(anyString());
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("update should throw CONFLICT when changing to an existing code")
    void update_codeConflict() {
        VoucherRequest request = buildRequest("FLASH50", VoucherType.PERCENTAGE, "50");

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.existsByCodeIgnoreCase("FLASH50")).thenReturn(true);

        assertThatThrownBy(() -> service.update(voucherId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw NOT_FOUND when voucher missing")
    void update_notFound() {
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(voucherId, buildRequest("X", VoucherType.PERCENTAGE, "10")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    // ─── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete should remove voucher when exists")
    void delete_success() {
        when(voucherRepository.existsById(voucherId)).thenReturn(true);

        service.delete(voucherId);

        verify(voucherRepository).deleteById(voucherId);
    }

    @Test
    @DisplayName("delete should throw NOT_FOUND when voucher missing")
    void delete_notFound() {
        when(voucherRepository.existsById(voucherId)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(voucherId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(voucherRepository, never()).deleteById(any());
    }

    // ─── apply ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("apply should return correct percentage discount")
    void apply_percentage_success() {
        // 10% of 200,000 = 20,000
        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        ApplyVoucherResponse result = service.apply("SAVE10", new BigDecimal("200000"));

        assertThat(result.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("20000"));
    }

    @Test
    @DisplayName("apply should cap percentage discount at maxDiscountAmount")
    void apply_percentage_cappedByMax() {
        voucher.setValue(new BigDecimal("20")); // 20%
        voucher.setMaxDiscountAmount(new BigDecimal("30000")); // cap at 30,000
        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        // 20% of 300,000 = 60,000 → capped at 30,000
        ApplyVoucherResponse result = service.apply("SAVE10", new BigDecimal("300000"));

        assertThat(result.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("30000"));
    }

    @Test
    @DisplayName("apply should return correct fixed-amount discount")
    void apply_fixedAmount_success() {
        voucher.setType(VoucherType.FIXED_AMOUNT);
        voucher.setValue(new BigDecimal("50000"));
        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        ApplyVoucherResponse result = service.apply("SAVE10", new BigDecimal("200000"));

        assertThat(result.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    @DisplayName("apply fixed-amount should not exceed order total")
    void apply_fixedAmount_cannotExceedTotal() {
        voucher.setType(VoucherType.FIXED_AMOUNT);
        voucher.setValue(new BigDecimal("500000")); // larger than order
        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        ApplyVoucherResponse result = service.apply("SAVE10", new BigDecimal("100000"));

        assertThat(result.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    @DisplayName("apply should throw BAD_REQUEST when code not found")
    void apply_codeNotFound() {
        when(voucherRepository.findByCodeIgnoreCase("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply("INVALID", new BigDecimal("100000")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("apply should throw BAD_REQUEST when voucher is inactive")
    void apply_inactive() {
        voucher.setActive(false);
        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.apply("SAVE10", new BigDecimal("100000")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("apply should throw BAD_REQUEST when voucher is expired")
    void apply_expired() {
        voucher.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.apply("SAVE10", new BigDecimal("100000")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("apply should not throw when expiry is in the future")
    void apply_notYetExpired() {
        voucher.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        ApplyVoucherResponse result = service.apply("SAVE10", new BigDecimal("100000"));

        assertThat(result.getDiscountAmount()).isNotNull();
    }

    @Test
    @DisplayName("apply should throw BAD_REQUEST when usage limit is reached")
    void apply_usageLimitReached() {
        voucher.setUsageLimit(5);
        voucher.setTimesUsed(5);
        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.apply("SAVE10", new BigDecimal("100000")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("apply should not throw when usage limit is not yet reached")
    void apply_usageLimitNotReached() {
        voucher.setUsageLimit(5);
        voucher.setTimesUsed(4);
        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        ApplyVoucherResponse result = service.apply("SAVE10", new BigDecimal("100000"));

        assertThat(result.getDiscountAmount()).isNotNull();
    }

    @Test
    @DisplayName("apply should throw BAD_REQUEST when order total is below minimum")
    void apply_belowMinOrder() {
        voucher.setMinOrderAmount(new BigDecimal("200000"));
        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.apply("SAVE10", new BigDecimal("100000")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    // ─── incrementUsage ────────────────────────────────────────────────────────

    @Test
    @DisplayName("incrementUsage should increment timesUsed by 1 and save")
    void incrementUsage_success() {
        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> inv.getArgument(0));

        service.incrementUsage("SAVE10");

        assertThat(voucher.getTimesUsed()).isEqualTo(1);
        verify(voucherRepository).save(voucher);
    }

    @Test
    @DisplayName("incrementUsage should be no-op when code not found")
    void incrementUsage_codeNotFound() {
        when(voucherRepository.findByCodeIgnoreCase("MISSING")).thenReturn(Optional.empty());

        service.incrementUsage("MISSING");

        verify(voucherRepository, never()).save(any());
    }

    // ─── helper ────────────────────────────────────────────────────────────────

    private VoucherRequest buildRequest(String code, VoucherType type, String value) {
        VoucherRequest req = new VoucherRequest();
        req.setCode(code);
        req.setType(type);
        req.setValue(new BigDecimal(value));
        req.setMinOrderAmount(BigDecimal.ZERO);
        req.setActive(true);
        return req;
    }
}
