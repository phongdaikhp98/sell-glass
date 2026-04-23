package com.sellglass.voucher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VoucherRepository extends JpaRepository<Voucher, UUID> {

    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Query("SELECT v FROM Voucher v WHERE " +
           "(:search IS NULL OR LOWER(v.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Voucher> search(@Param("search") String search, Pageable pageable);
}
