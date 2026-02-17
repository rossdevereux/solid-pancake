package com.voucher.api.controller;

import com.voucher.api.domain.Voucher;
import com.voucher.api.service.VoucherService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody ValidateRequest request) {
        Voucher voucher = voucherService.validate(request.getCode());
        return ResponseEntity.ok(Map.of("valid", true, "voucher", voucher));
    }

    @PostMapping("/redeem")
    public ResponseEntity<?> redeem(@RequestBody RedeemRequest request) {
        Voucher voucher = voucherService.redeem(request.getCode(), request.getUserId());
        return ResponseEntity.ok(voucher);
    }

    @Data
    public static class ValidateRequest {
        private String code;
    }

    @Data
    public static class RedeemRequest {
        private String code;
        private String userId;
    }
}
