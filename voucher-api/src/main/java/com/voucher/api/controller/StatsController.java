package com.voucher.api.controller;

import com.voucher.api.domain.Voucher;
import com.voucher.api.domain.VoucherBatch;
import com.voucher.api.repository.VoucherBatchRepository;
import com.voucher.api.repository.VoucherRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final VoucherRepository voucherRepository;
    private final VoucherBatchRepository batchRepository;

    @GetMapping
    public DashboardStats getStats() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        String orgId = com.voucher.api.config.TenantContext.getTenantId();

        return DashboardStats.builder()
                .totalVouchers(voucherRepository.countByOrgId(orgId))
                .activeBatches(batchRepository.countByStatusAndOrgId(VoucherBatch.BatchStatus.ACTIVE, orgId))
                .redeemedToday(voucherRepository.countByOrgIdAndRedemptionsRedeemedAtAfter(orgId, todayStart))
                .activeVouchers(voucherRepository.countByStatusAndOrgId(Voucher.VoucherStatus.ACTIVE, orgId))
                .build();
    }

    @Data
    @Builder
    public static class DashboardStats {
        private long totalVouchers;
        private long activeBatches;
        private long redeemedToday;
        private long activeVouchers;
    }
}
