package com.voucher.user.controller;

import tools.jackson.databind.ObjectMapper;
import com.voucher.core.domain.Voucher;
import com.voucher.user.service.VoucherUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class VoucherControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @MockitoBean
    private VoucherUserService voucherUserService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testValidate() throws Exception {
        Voucher voucher = new Voucher();
        voucher.setCodeHash("hash");
        voucher.setStatus(Voucher.VoucherStatus.ACTIVE);
        voucher.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(voucherUserService.validate(anyString())).thenReturn(voucher);

        VoucherController.ValidateRequest request = new VoucherController.ValidateRequest();
        request.setCode("TEST-CODE");

        mockMvc.perform(post("/api/v1/vouchers/validate")
                .with(jwt().jwt(j -> j.claim("orgId", "TEST-ORG")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void testRedeem() throws Exception {
        Voucher voucher = new Voucher();
        voucher.setCodeHash("hash");
        voucher.setStatus(Voucher.VoucherStatus.REDEEMED);
        voucher.setExpiryDate(LocalDateTime.now().plusDays(1));
        voucher.setUsageCount(1);
        voucher.setMaxUsage(1);

        when(voucherUserService.redeem(anyString(), anyString())).thenReturn(voucher);

        VoucherController.RedeemRequest request = new VoucherController.RedeemRequest();
        request.setCode("TEST-CODE");
        request.setUserId("user1");

        mockMvc.perform(post("/api/v1/vouchers/redeem")
                .with(jwt().jwt(j -> j.claim("orgId", "TEST-ORG")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REDEEMED"))
                .andExpect(jsonPath("$.usageCount").value(1));
    }
}
