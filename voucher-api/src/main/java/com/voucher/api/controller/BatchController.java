package com.voucher.api.controller;

import com.voucher.api.domain.VoucherBatch;
import com.voucher.api.service.BatchService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<VoucherBatch> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(batchService.getBatch(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public VoucherBatch create(@RequestBody CreateBatchRequest request) {
        return batchService.createBatch(request.getTemplateId(), request.getCount());
    }

    @PutMapping("/{id}")
    public ResponseEntity<VoucherBatch> updateStatus(@PathVariable String id,
            @RequestBody UpdateStatusRequest request) {
        try {
            return ResponseEntity.ok(batchService.updateStatus(id, request.getStatus()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
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
