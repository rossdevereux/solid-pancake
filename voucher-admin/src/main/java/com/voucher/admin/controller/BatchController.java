package com.voucher.admin.controller;

import com.voucher.core.domain.VoucherBatch;
import com.voucher.admin.service.BatchService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @GetMapping
    public List<VoucherBatch> getAll() {
        return batchService.getAllBatches();
    }

    @GetMapping("/{id}")
    public VoucherBatch getById(@PathVariable String id) {
        return batchService.getBatch(id);
    }

    @PostMapping
    public VoucherBatch create(@RequestBody CreateBatchRequest request) {
        return batchService.createBatch(request.getTemplateId(), request.getCount());
    }

    @PutMapping("/{id}")
    public VoucherBatch updateStatus(@PathVariable String id,
            @RequestBody UpdateStatusRequest request) {
        return batchService.updateStatus(id, request.getStatus());
    }

    @Data
    public static class CreateBatchRequest {
        private String templateId;
        private int count;
    }

    @Data
    public static class UpdateStatusRequest {
        private VoucherBatch.BatchStatus status;
    }
}
