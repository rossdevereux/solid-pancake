package com.voucher.core.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "daily_stats")
@CompoundIndex(name = "org_date_idx", def = "{'orgId': 1, 'date': 1}", unique = true)
public class DailyStats {
    @Id
    private String id;
    private String orgId;
    private String date; // Format: YYYY-MM-DD
    private long totalRedeemed;
    private long totalGenerated;
}
