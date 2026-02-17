package com.voucher.api.service;

import com.voucher.api.domain.Voucher;
import com.voucher.api.domain.VoucherBatch;
import com.voucher.api.domain.VoucherTemplate;
import com.voucher.api.exception.ValidationException;
import com.voucher.api.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final EncryptionService encryptionService;
    private final StringRedisTemplate redisTemplate;

    // Pattern characters
    private static final String CHAR_NUMERIC = "0123456789";
    private static final String CHAR_ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String CHAR_ALPHANUMERIC = CHAR_ALPHA + CHAR_NUMERIC;

    // Redis Set key for all voucher hashes
    private static final String REDIS_VOUCHER_HASHES_KEY = "voucher:hashes";

    @Transactional
    public void generateVouchers(VoucherBatch batch, VoucherTemplate template) {
        int count = batch.getQuantity();
        String pattern = template.getCodeFormat();

        int totalGenerated = 0;
        int attempts = 0;
        int maxAttempts = 100;

        while (totalGenerated < count && attempts < maxAttempts) {
            int needed = count - totalGenerated;
            // Batch size 5000 for better performance
            int batchSize = Math.min(needed, 5000);

            // 1 & 2. Generate candidates in parallel (Generation + Hashing + Encryption)
            List<VoucherCandidate> candidates = java.util.stream.Stream.generate(() -> {
                String code = generateCode(pattern);
                String h = hash(code);
                String enc = encryptionService.encrypt(code);
                return new VoucherCandidate(code, h, enc);
            })
                    .parallel() // MULTI-CORE POWER!
                    .limit(batchSize)
                    .collect(java.util.stream.Collectors.toList());

            // 3. Filter unique via Redis Pipeline
            byte[] redisKey = REDIS_VOUCHER_HASHES_KEY.getBytes(StandardCharsets.UTF_8);
            List<Object> results = redisTemplate
                    .executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                        for (VoucherCandidate c : candidates) {
                            connection.setCommands().sAdd(redisKey, c.hash.getBytes(StandardCharsets.UTF_8));
                        }
                        return null;
                    });

            // 4. Process results and save to DB
            List<Voucher> batchVouchers = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                Long result = (Long) results.get(i);
                if (result != null && result == 1) {
                    VoucherCandidate c = candidates.get(i);
                    Voucher voucher = new Voucher();
                    voucher.setOrgId(batch.getOrgId()); // Use batch's orgId
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

        if (totalGenerated < count) {
            throw new RuntimeException("Failed to generate unique codes. Pattern might be exhausted.");
        }
    }

    private static class VoucherCandidate {
        final String code;
        final String hash;
        final String encrypted;

        VoucherCandidate(String code, String hash, String encrypted) {
            this.code = code;
            this.hash = hash;
            this.encrypted = encrypted;
        }
    }

    private static final String ALPHABET = CHAR_NUMERIC + CHAR_ALPHA;

    private String generateCode(String pattern) {
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        boolean explicitCheckDigit = pattern.contains("*");

        for (char c : pattern.toCharArray()) {
            if (c == '#') {
                sb.append(CHAR_NUMERIC.charAt(random.nextInt(CHAR_NUMERIC.length())));
            } else if (c == '?') {
                sb.append(CHAR_ALPHANUMERIC.charAt(random.nextInt(CHAR_ALPHANUMERIC.length())));
            } else if (c == '*') {
                // Placeholder for check digit - will be filled later
                sb.append('*');
            } else {
                sb.append(c);
            }
        }

        String code = sb.toString();
        char checkDigit = calculateCheckDigit(code.replace("*", ""));

        if (explicitCheckDigit) {
            return code.replace('*', checkDigit);
        } else {
            return code + checkDigit;
        }
    }

    private char calculateCheckDigit(String input) {
        String cleanInput = input.toUpperCase().replaceAll("[^0-9A-Z]", "");
        int sum = 0;
        int n = ALPHABET.length();

        for (int i = 0; i < cleanInput.length(); i++) {
            int val = ALPHABET.indexOf(cleanInput.charAt(i));
            // Weight: 2 for even positions (from right), 1 for odd
            int factor = ((cleanInput.length() - i) % 2 == 0) ? 2 : 1;
            int add = val * factor;
            sum += (add / n) + (add % n);
        }

        int remainder = sum % n;
        int checkIdx = (n - remainder) % n;
        return ALPHABET.charAt(checkIdx);
    }

    public boolean validateCheckDigit(String code) {
        if (code == null || code.length() < 2)
            return false;
        String cleanCode = code.toUpperCase().replaceAll("[^0-9A-Z]", "");
        if (cleanCode.length() < 2)
            return false;

        char provided = cleanCode.charAt(cleanCode.length() - 1);
        String data = cleanCode.substring(0, cleanCode.length() - 1);
        return calculateCheckDigit(data) == provided;
    }

    @Transactional
    public Voucher redeem(String code, String userId) {
        // Reuse validation logic
        Voucher voucher = validate(code);

        // Record redemption
        Voucher.Redemption redemption = new Voucher.Redemption();
        redemption.setUserId(userId);
        redemption.setRedeemedAt(LocalDateTime.now());
        voucher.getRedemptions().add(redemption);

        voucher.setUsageCount(voucher.getUsageCount() + 1);

        if (voucher.getUsageCount() >= voucher.getMaxUsage()) {
            voucher.setStatus(Voucher.VoucherStatus.REDEEMED);
        }

        return voucherRepository.save(voucher);
    }

    public Voucher validate(String code) {
        String h = hash(code);
        Voucher voucher = voucherRepository
                .findByCodeHashAndOrgId(h, com.voucher.api.config.TenantContext.getTenantId())
                .orElseThrow(() -> new ValidationException("Voucher not found"));

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

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
