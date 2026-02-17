package com.voucher.user.service;

import com.voucher.core.config.RabbitStatsConfig;
import com.voucher.core.domain.Voucher;
import com.voucher.core.domain.VoucherStatsMessage;
import com.voucher.core.exception.ValidationException;
import com.voucher.core.exception.ResourceNotFoundException;
import com.voucher.core.repository.VoucherRepository;
import com.voucher.core.service.VoucherCoreService;
import com.voucher.core.config.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class VoucherUserService {

    private final VoucherRepository voucherRepository;
    private final VoucherCoreService voucherCoreService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Voucher redeem(String code, String userId) {
        Voucher voucher = validate(code);

        Voucher.Redemption redemption = new Voucher.Redemption();
        redemption.setUserId(userId);
        redemption.setRedeemedAt(LocalDateTime.now());
        voucher.getRedemptions().add(redemption);

        voucher.setUsageCount(voucher.getUsageCount() + 1);

        if (voucher.getUsageCount() >= voucher.getMaxUsage()) {
            voucher.setStatus(Voucher.VoucherStatus.REDEEMED);
        }

        Voucher savedVoucher = voucherRepository.save(voucher);

        // Publish stats message
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        VoucherStatsMessage statsMessage = VoucherStatsMessage.builder()
                .orgId(voucher.getOrgId())
                .type(VoucherStatsMessage.StatsType.REDEMPTION)
                .count(1)
                .date(today)
                .build();
        
        rabbitTemplate.convertAndSend(RabbitStatsConfig.VOUCHER_STATS_EXCHANGE, RabbitStatsConfig.VOUCHER_STATS_ROUTING_KEY, statsMessage);

        return savedVoucher;
    }

    public Voucher validate(String code) {
        String h = voucherCoreService.hash(code);
        Voucher voucher = voucherRepository
                .findByCodeHashAndOrgId(h, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        if (voucher.getStatus() != Voucher.VoucherStatus.ACTIVE) {
            throw new ValidationException("Voucher is not active");
        }

        if (voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Voucher expired");
        }

        if (voucher.getUsageCount() >= voucher.getMaxUsage()) {
            throw new ValidationException("Voucher usage limit reached");
        }

        return voucher;
    }
}
