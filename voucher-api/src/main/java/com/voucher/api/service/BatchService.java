package com.voucher.api.service;

import com.voucher.api.domain.VoucherBatch;
import com.voucher.api.domain.VoucherTemplate;
import com.voucher.api.repository.VoucherBatchRepository;
import com.voucher.api.repository.VoucherTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private final VoucherBatchRepository batchRepository;
    private final VoucherTemplateRepository templateRepository;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Transactional
    public VoucherBatch createBatch(String templateId, int count) {
        String orgId = com.voucher.api.config.TenantContext.getTenantId();
        log.debug("createBatch templateId={} orgId={}", templateId, orgId);
        VoucherTemplate template = templateRepository.findByIdAndOrgId(templateId, orgId)
                .orElseThrow(() -> new com.voucher.api.exception.ResourceNotFoundException("Template not found"));

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

        // Send task to RabbitMQ for async generation
        com.voucher.api.dto.VoucherGenerationTask task = new com.voucher.api.dto.VoucherGenerationTask(
                batch.getId(), template.getId(), orgId);
        rabbitTemplate.convertAndSend(
                com.voucher.api.config.RabbitMQConfig.VOUCHER_GEN_EXCHANGE,
                com.voucher.api.config.RabbitMQConfig.VOUCHER_GEN_ROUTING_KEY,
                task);

        return batch;
    }

    public List<VoucherBatch> getAllBatches() {
        return batchRepository.findByOrgId(com.voucher.api.config.TenantContext.getTenantId());
    }

    public VoucherBatch getBatch(String id) {
        return batchRepository.findByIdAndOrgId(id, com.voucher.api.config.TenantContext.getTenantId())
                .orElseThrow(() -> new com.voucher.api.exception.ResourceNotFoundException("Batch not found"));
    }

    public VoucherBatch updateStatus(String id, VoucherBatch.BatchStatus status) {
        if (status == VoucherBatch.BatchStatus.GENERATING) {
            throw new com.voucher.api.exception.ValidationException("Cannot manually update status to GENERATING");
        }

        String orgId = com.voucher.api.config.TenantContext.getTenantId();
        VoucherBatch batch = batchRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new com.voucher.api.exception.ResourceNotFoundException("Batch not found"));

        batch.setStatus(status);
        return batchRepository.save(batch);
    }
}
