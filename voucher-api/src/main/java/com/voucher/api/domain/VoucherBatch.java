package com.voucher.api.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "batches")
public class VoucherBatch {
    @Id
    private String id;

    @Indexed
    private String orgId;

    private String templateId;
    private BatchStatus status = BatchStatus.PENDING;
    private int quantity;
    private LocalDateTime createdDate = LocalDateTime.now();
    private LocalDateTime expiryDate;

    public enum BatchStatus {
        PENDING, GENERATING, ACTIVE, EXPIRED, ARCHIVED
    }
}
