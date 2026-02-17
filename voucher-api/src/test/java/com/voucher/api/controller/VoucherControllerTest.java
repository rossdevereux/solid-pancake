package com.voucher.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.voucher.api.domain.Voucher;
import com.voucher.api.service.VoucherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoucherController.class)
class VoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VoucherService voucherService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testValidate() throws Exception {
        Voucher voucher = new Voucher();
        voucher.setCodeHash("hash");
        voucher.setStatus(Voucher.VoucherStatus.ACTIVE);
        voucher.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(voucherService.validate(anyString())).thenReturn(voucher);

        VoucherController.ValidateRequest request = new VoucherController.ValidateRequest();
        request.setCode("TEST-CODE");

        mockMvc.perform(post("/api/v1/vouchers/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.voucher.status").value("ACTIVE"));
    }

    @Test
    void testRedeem() throws Exception {
        Voucher voucher = new Voucher();
        voucher.setCodeHash("hash");
        voucher.setStatus(Voucher.VoucherStatus.REDEEMED);
        voucher.setExpiryDate(LocalDateTime.now().plusDays(1));
        voucher.setUsageCount(1);
        voucher.setMaxUsage(1);

        when(voucherService.redeem(anyString(), anyString())).thenReturn(voucher);

        VoucherController.RedeemRequest request = new VoucherController.RedeemRequest();
        request.setCode("TEST-CODE");
        request.setUserId("user1");

        mockMvc.perform(post("/api/v1/vouchers/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REDEEMED"))
                .andExpect(jsonPath("$.usageCount").value(1));
    }
}
