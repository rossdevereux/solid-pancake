package com.voucher.admin.service;

import com.voucher.core.domain.Voucher;
import com.voucher.core.domain.VoucherBatch;
import com.voucher.core.domain.VoucherTemplate;
import com.voucher.core.repository.VoucherRepository;
import com.voucher.core.service.EncryptionService;
import com.voucher.core.service.VoucherCoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherAdminService {

    private final VoucherRepository voucherRepository;
    private final EncryptionService encryptionService;
    private final StringRedisTemplate redisTemplate;
    private final VoucherCoreService voucherCoreService;

    private static final String REDIS_VOUCHER_HASHES_KEY = "voucher:hashes";

    @Transactional
    public int generateVouchers(VoucherBatch batch, VoucherTemplate template, int countToGenerate) {
        String pattern = template.getCodeFormat();

        int totalGenerated = 0;
        int attempts = 0;
        int maxAttempts = 10;

        while (totalGenerated < countToGenerate && attempts < maxAttempts) {
            int needed = countToGenerate - totalGenerated;
            int batchSize = Math.min(needed, 1000);

            List<VoucherCandidate> candidates = Stream.generate(() -> {
                String code = voucherCoreService.generateCode(pattern);
                String h = voucherCoreService.hash(code);
                String enc = encryptionService.encrypt(code);
                return new VoucherCandidate(h, enc);
            })
                    .parallel()
                    .limit(batchSize)
                    .collect(Collectors.toList());

            byte[] redisKey = REDIS_VOUCHER_HASHES_KEY.getBytes(StandardCharsets.UTF_8);
            List<Object> results = redisTemplate
                    .executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                        for (VoucherCandidate c : candidates) {
                            connection.setCommands().sAdd(redisKey, c.hash.getBytes(StandardCharsets.UTF_8));
                        }
                        return null;
                    });

            List<Voucher> batchVouchers = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                Long result = (Long) results.get(i);
                if (result != null && result == 1) {
                    VoucherCandidate c = candidates.get(i);
                    Voucher voucher = new Voucher();
                    voucher.setOrgId(batch.getOrgId());
                    voucher.setBatchId(batch.getId());
                    voucher.setEncryptedCode(c.encrypted);
                    voucher.setCodeHash(c.hash);

                    int limit = 1;
                    if (template.getRedemptionLimit() != null && template.getRedemptionLimit().getLimitPerUser() > 0) {
                        limit = template.getRedemptionLimit().getLimitPerUser();
                    }
                    voucher.setMaxUsage(limit);
                    voucher.setExpiryDate(batch.getExpiryDate());

                    batchVouchers.add(voucher);
                }
            }

            if (!batchVouchers.isEmpty()) {
                voucherRepository.saveAll(batchVouchers);
                totalGenerated += batchVouchers.size();
                attempts = 0;
            } else {
                attempts++;
            }
        }

        return totalGenerated;
    }

    private static class VoucherCandidate {
        final String hash;
        final String encrypted;

        VoucherCandidate(String hash, String encrypted) {
            this.hash = hash;
            this.encrypted = encrypted;
        }
    }
}
