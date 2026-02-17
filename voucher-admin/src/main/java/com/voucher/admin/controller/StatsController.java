package com.voucher.admin.controller;

import com.voucher.core.domain.Voucher;
import com.voucher.core.repository.VoucherBatchRepository;
import com.voucher.core.repository.VoucherRepository;
import com.voucher.core.config.TenantContext;
import com.voucher.core.domain.VoucherBatch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final VoucherRepository voucherRepository;
    private final VoucherBatchRepository batchRepository;

    @GetMapping
    public Map<String, Object> getStats() {
        String orgId = TenantContext.getTenantId();
        Map<String, Object> stats = new LinkedHashMap<>();
        
        stats.put("totalVouchers", voucherRepository.countByOrgId(orgId));
        stats.put("activeVouchers", voucherRepository.countByStatusAndOrgId(Voucher.VoucherStatus.ACTIVE, orgId));
        stats.put("redeemedVouchers", voucherRepository.countByStatusAndOrgId(Voucher.VoucherStatus.REDEEMED, orgId));
        
        long pendingBatches = batchRepository.countByStatusAndOrgId(VoucherBatch.BatchStatus.PENDING, orgId);
        long generatingBatches = batchRepository.countByStatusAndOrgId(VoucherBatch.BatchStatus.GENERATING, orgId);
        stats.put("pendingBatches", pendingBatches + generatingBatches);
        
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        stats.put("redemptionsToday", voucherRepository.countByOrgIdAndRedemptionsRedeemedAtAfter(orgId, today));
        
        return stats;
    }
}
