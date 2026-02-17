package com.voucher.user;

import com.voucher.core.domain.Voucher;
import com.voucher.core.repository.VoucherRepository;
import com.voucher.core.service.EncryptionService;
import com.voucher.core.service.VoucherCoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class MultiTenancyIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherCoreService voucherCoreService;

    @Autowired
    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        voucherRepository.deleteAll();
    }

    @Test
    void testTenantIsolation() throws Exception {
        String org1 = "ORG-1";
        String org2 = "ORG-2";
        String code = "SHARED-CODE-123";

        // 1. Create voucher for ORG-1
        Voucher v1 = new Voucher();
        v1.setOrgId(org1);
        v1.setCodeHash(voucherCoreService.hash(code));
        v1.setEncryptedCode(encryptionService.encrypt(code));
        v1.setMaxUsage(1);
        voucherRepository.save(v1);

        // 2. Try to validate with ORG-2 token
        com.voucher.user.controller.VoucherController.ValidateRequest request = new com.voucher.user.controller.VoucherController.ValidateRequest();
        request.setCode(code);

        mockMvc.perform(post("/api/v1/vouchers/validate")
                .with(jwt().jwt(j -> j.claim("orgId", org2)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\": \"" + code + "\"}"))
                .andExpect(status().isNotFound());

        // 3. Validate with ORG-1 token
        mockMvc.perform(post("/api/v1/vouchers/validate")
                .with(jwt().jwt(j -> j.claim("orgId", org1)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\": \"" + code + "\"}"))
                .andExpect(status().isOk());
    }
}
