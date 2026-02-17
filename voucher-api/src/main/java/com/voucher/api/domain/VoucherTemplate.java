package com.voucher.api.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "templates")
public class VoucherTemplate {
    @Id
    private String id;

    @Indexed
    private String orgId;

    private String name;
    private String description;
    private String codeFormat; // e.g., "SUMMER-XXXX-YYYY"

    private ValidityPeriod validityPeriod;
    private RedemptionLimit redemptionLimit;
    private VoucherConstraints constraints;

    private LocalDateTime createdDate = LocalDateTime.now();

    @Data
    public static class ValidityPeriod {
        private PeriodType type;
        private Integer durationDays; // Used if type is DURATION
        private LocalDateTime startDate; // Used if type is DATE_RANGE
        private LocalDateTime endDate; // Used if type is DATE_RANGE

        public enum PeriodType {
            DURATION, DATE_RANGE
        }
    }

    @Data
    public static class RedemptionLimit {
        private int limitPerUser;
        private int limitOverall;
    }

    @Data
    public static class VoucherConstraints {
        private BigDecimal minPurchaseAmount;
        private List<String> eligibleProducts;
    }
}
