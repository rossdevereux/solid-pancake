package com.voucher.admin.controller;

import com.voucher.core.domain.Voucher;
import com.voucher.core.repository.VoucherBatchRepository;
import com.voucher.core.repository.VoucherRepository;
import com.voucher.core.repository.VoucherStatsRepository;
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

    private final VoucherBatchRepository batchRepository; // Keep for now as pending/generating isn't tied to GlobalStats exactly same way or could be but let's stick to the plan
    private final VoucherStatsRepository voucherStatsRepository;

    @GetMapping
    public Map<String, Object> getStats() {
        String orgId = TenantContext.getTenantId();
        Map<String, Object> stats = new LinkedHashMap<>();
        
        // Fetch from GlobalStats
        var globalStats = voucherStatsRepository.findGlobalStatsByOrgId(orgId).orElse(null);
        long totalVouchers = globalStats != null ? globalStats.getTotalVouchers() : 0;
        long activeVouchers = globalStats != null ? globalStats.getActiveVouchers() : 0;
        long redeemedVouchers = globalStats != null ? globalStats.getRedeemedVouchers() : 0;

        stats.put("totalVouchers", totalVouchers);
        stats.put("activeVouchers", activeVouchers);
        stats.put("redeemedVouchers", redeemedVouchers);
        
        // Still fetch batch stats from repository as these are transient states not fully tracked in global stats yet? 
        // Or should we? The plan said "Refactor reporting API to query these pre-computed documents".
        // Pending/Generating batches are "in flight" so querying batch repo is fine and fast enough (usually few batches).
        long pendingBatches = batchRepository.countByStatusAndOrgId(VoucherBatch.BatchStatus.PENDING, orgId);
        long generatingBatches = batchRepository.countByStatusAndOrgId(VoucherBatch.BatchStatus.GENERATING, orgId);
        stats.put("pendingBatches", pendingBatches + generatingBatches);
        
        // Fetch today's redemptions from DailyStats
        String today = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        long redemptionsToday = voucherStatsRepository.getRedemptionsToday(orgId, today);
        stats.put("redemptionsToday", redemptionsToday);
        
        return stats;
    }
}
