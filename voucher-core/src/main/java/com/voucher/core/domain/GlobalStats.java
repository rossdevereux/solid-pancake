package com.voucher.core.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "global_stats")
public class GlobalStats {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String orgId;
    
    private long totalVouchers;
    private long activeVouchers;
    private long redeemedVouchers;
}
