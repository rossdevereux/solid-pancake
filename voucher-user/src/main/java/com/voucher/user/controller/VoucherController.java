package com.voucher.user.controller;

import com.voucher.core.domain.Voucher;
import com.voucher.user.service.VoucherUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherUserService voucherService;

    @PostMapping("/validate")
    public Voucher validate(@RequestBody ValidateRequest request) {
        return voucherService.validate(request.getCode());
    }

    @PostMapping("/redeem")
    public Voucher redeem(@RequestBody RedeemRequest request) {
        return voucherService.redeem(request.getCode(), request.getUserId());
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
