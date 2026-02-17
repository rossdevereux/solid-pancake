package com.voucher.core.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "vouchers")
public class Voucher {
    @Id
    private String id;

    @Indexed
    private String orgId;

    @Indexed
    private String batchId;

    // Encrypted code stored here
    private String encryptedCode;

    // Hashed code for lookups (SHA-256)
    @Indexed(unique = true)
    private String codeHash;

    private VoucherStatus status = VoucherStatus.ACTIVE;
    private int usageCount = 0;
    private int maxUsage = 1;

    private LocalDateTime createdDate = LocalDateTime.now();
    private LocalDateTime expiryDate;
    private List<Redemption> redemptions = new ArrayList<>();

    @Data
    public static class Redemption {
        private String userId;
        private LocalDateTime redeemedAt = LocalDateTime.now();
    }

    public enum VoucherStatus {
        ACTIVE, REDEEMED, EXPIRED, VOID
    }
}
