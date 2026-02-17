package com.voucher.api.service;

import com.voucher.api.domain.VoucherBatch;
import com.voucher.api.domain.VoucherTemplate;
import com.voucher.api.repository.VoucherBatchRepository;
import com.voucher.api.repository.VoucherTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final VoucherBatchRepository batchRepository;
    private final VoucherTemplateRepository templateRepository;
    private final VoucherService voucherService;

    @Transactional
    public VoucherBatch createBatch(String templateId, int count) {
        String orgId = com.voucher.api.config.TenantContext.getTenantId();
        VoucherTemplate template = templateRepository.findByIdAndOrgId(templateId, orgId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        VoucherBatch batch = new VoucherBatch();
        batch.setOrgId(orgId);
        batch.setTemplateId(templateId);
        batch.setQuantity(count);
        batch.setStatus(VoucherBatch.BatchStatus.GENERATING);
        batch.setCreatedDate(LocalDateTime.now());

        // simple expiry logic
        if (template.getValidityPeriod() != null) {
            if (template.getValidityPeriod().getType() == VoucherTemplate.ValidityPeriod.PeriodType.DURATION
                    && template.getValidityPeriod().getDurationDays() != null) {
                batch.setExpiryDate(batch.getCreatedDate().plusDays(template.getValidityPeriod().getDurationDays()));
            } else if (template.getValidityPeriod().getType() == VoucherTemplate.ValidityPeriod.PeriodType.DATE_RANGE
                    && template.getValidityPeriod().getEndDate() != null) {
                batch.setExpiryDate(template.getValidityPeriod().getEndDate());
            }
        }

        batch = batchRepository.save(batch);

        // Async generation could be handled here with @Async or a job queue
        // For now, synchronous
        voucherService.generateVouchers(batch, template);

        batch.setStatus(VoucherBatch.BatchStatus.ACTIVE);
        return batchRepository.save(batch);
    }

    public List<VoucherBatch> getAllBatches() {
        return batchRepository.findByOrgId(com.voucher.api.config.TenantContext.getTenantId());
    }

    public VoucherBatch getBatch(String id) {
        return batchRepository.findByIdAndOrgId(id, com.voucher.api.config.TenantContext.getTenantId())
                .orElseThrow(() -> new RuntimeException("Batch not found"));
    }

    @Transactional
    public VoucherBatch updateStatus(String id, VoucherBatch.BatchStatus status) {
        VoucherBatch batch = getBatch(id);
        batch.setStatus(status);
        return batchRepository.save(batch);
    }
}
