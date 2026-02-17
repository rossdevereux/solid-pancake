package com.voucher.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherGenerationTask {
    private String batchId;
    private String templateId;
    private String orgId;
}
