package com.voucher.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherStatsMessage implements Serializable {
    private String orgId;
    private StatsType type;
    private int count;
    private String date; // YYYY-MM-DD

    public enum StatsType {
        GENERATION, REDEMPTION
    }
}
