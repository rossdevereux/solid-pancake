package com.voucher.api.service;

import com.voucher.api.config.RabbitMQConfig;
import com.voucher.api.domain.VoucherBatch;
import com.voucher.api.domain.VoucherTemplate;
import com.voucher.api.dto.VoucherGenerationTask;
import com.voucher.api.repository.VoucherBatchRepository;
import com.voucher.api.repository.VoucherTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherGenerationListener {

    private final VoucherService voucherService;
    private final VoucherBatchRepository batchRepository;
    private final VoucherTemplateRepository templateRepository;

    @RabbitListener(queues = RabbitMQConfig.VOUCHER_GEN_QUEUE)
    public void handleGeneration(VoucherGenerationTask task) {
        log.info("Starting voucher generation for batch: {}", task.getBatchId());

        VoucherBatch batch = batchRepository.findById(task.getBatchId()).orElse(null);
        VoucherTemplate template = templateRepository.findById(task.getTemplateId()).orElse(null);

        if (batch == null || template == null) {
            log.error("Batch or template not found for task: {}", task);
            return;
        }

        try {
            int totalGenerated = batch.getGeneratedCount();
            int target = batch.getQuantity();
            int chunkSize = 1000;

            while (totalGenerated < target) {
                int toGen = Math.min(chunkSize, target - totalGenerated);
                int actuallyGenerated = voucherService.generateVouchers(batch, template, toGen);

                if (actuallyGenerated == 0) {
                    log.error("Failed to generate any vouchers in chunk for batch: {}. Pattern might be exhausted.",
                            batch.getId());
                    batch.setStatus(VoucherBatch.BatchStatus.PENDING); // Or a FAILED status
                    batchRepository.save(batch);
                    return;
                }

                totalGenerated += actuallyGenerated;
                batch.setGeneratedCount(totalGenerated);
                batchRepository.save(batch);

                log.info("Progress for batch {}: {}/{}", batch.getId(), totalGenerated, target);
            }

            batch.setStatus(VoucherBatch.BatchStatus.ACTIVE);
            batchRepository.save(batch);
            log.info("Completed voucher generation for batch: {}", batch.getId());

        } catch (Exception e) {
            log.error("Error generating vouchers for batch: " + batch.getId(), e);
            // Optionally update batch status to error
        }
    }
}
