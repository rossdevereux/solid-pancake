package com.voucher.admin.controller;

import com.voucher.core.repository.VoucherBatchRepository;
import com.voucher.core.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class StatsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private VoucherRepository voucherRepository;

    @MockitoBean
    private VoucherBatchRepository batchRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testGetStats() throws Exception {
        when(voucherRepository.countByOrgId("TEST-ORG")).thenReturn(100L);
        when(voucherRepository.countByStatusAndOrgId(any(), anyString())).thenReturn(50L);
        when(batchRepository.countByStatusAndOrgId(any(), anyString())).thenReturn(5L);
        when(voucherRepository.countByOrgIdAndRedemptionsRedeemedAtAfter(anyString(), any())).thenReturn(10L);

        mockMvc.perform(get("/api/v1/stats")
                .with(jwt().jwt(j -> j.claim("orgId", "TEST-ORG"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVouchers").value(100))
                .andExpect(jsonPath("$.activeVouchers").value(50))
                .andExpect(jsonPath("$.redemptionsToday").value(10));
    }
}
