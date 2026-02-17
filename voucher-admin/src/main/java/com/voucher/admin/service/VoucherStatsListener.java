package com.voucher.admin.service;

import com.voucher.core.config.RabbitStatsConfig;
import com.voucher.core.domain.VoucherStatsMessage;
import com.voucher.core.repository.VoucherStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherStatsListener {

    private final VoucherStatsRepository voucherStatsRepository;

    @RabbitListener(queues = RabbitStatsConfig.VOUCHER_STATS_QUEUE)
    public void handleStatsUpdate(VoucherStatsMessage message) {
        log.info("Received stats update: {}", message);
        
        try {
            if (message.getType() == VoucherStatsMessage.StatsType.GENERATION) {
                voucherStatsRepository.incrementGenerationStats(message.getOrgId(), message.getDate(), message.getCount());
            } else if (message.getType() == VoucherStatsMessage.StatsType.REDEMPTION) {
                voucherStatsRepository.incrementRedemptionStats(message.getOrgId(), message.getDate());
            }
        } catch (Exception e) {
            log.error("Failed to update stats for message: {}", message, e);
            // In a real production scenario, we might want to dead-letter this message
            // or retry with backoff. For now, we log the error.
        }
    }
}
