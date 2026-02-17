package com.voucher.admin.service;

import com.voucher.core.domain.VoucherBatch;
import com.voucher.core.domain.VoucherTemplate;
import com.voucher.core.repository.VoucherBatchRepository;
import com.voucher.core.repository.VoucherTemplateRepository;
import com.voucher.core.exception.ResourceNotFoundException;
import com.voucher.core.exception.ValidationException;
import com.voucher.core.config.TenantContext;
import com.voucher.core.dto.VoucherGenerationTask;
import com.voucher.admin.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public VoucherBatch createBatch(String templateId, int count) {
        String orgId = TenantContext.getTenantId();
        log.debug("createBatch templateId={} orgId={}", templateId, orgId);
        VoucherTemplate template = templateRepository.findByIdAndOrgId(templateId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        VoucherBatch batch = new VoucherBatch();
        batch.setOrgId(orgId);
        batch.setTemplateId(templateId);
        batch.setQuantity(count);
        batch.setStatus(VoucherBatch.BatchStatus.GENERATING);
        batch.setCreatedDate(LocalDateTime.now());

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

        VoucherGenerationTask task = new VoucherGenerationTask(batch.getId(), template.getId(), orgId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.VOUCHER_GEN_EXCHANGE, RabbitMQConfig.VOUCHER_GEN_ROUTING_KEY, task);

        return batch;
    }

    public List<VoucherBatch> getAllBatches() {
        return batchRepository.findByOrgId(TenantContext.getTenantId());
    }

    public VoucherBatch getBatch(String id) {
        return batchRepository.findByIdAndOrgId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));
    }

    public VoucherBatch updateStatus(String id, VoucherBatch.BatchStatus status) {
        if (status == VoucherBatch.BatchStatus.GENERATING) {
            throw new ValidationException("Cannot manually update status to GENERATING");
        }

        String orgId = TenantContext.getTenantId();
        VoucherBatch batch = batchRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));

        batch.setStatus(status);
        return batchRepository.save(batch);
    }
}
