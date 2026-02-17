package com.voucher.user;

import tools.jackson.databind.ObjectMapper;
import com.voucher.core.domain.Voucher;
import com.voucher.core.domain.VoucherBatch;
import com.voucher.core.domain.VoucherTemplate;
import com.voucher.core.repository.VoucherBatchRepository;
import com.voucher.core.repository.VoucherRepository;
import com.voucher.core.repository.VoucherTemplateRepository;
import com.voucher.core.service.EncryptionService;
import com.voucher.core.service.VoucherCoreService;
import com.voucher.user.service.VoucherUserService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class VoucherIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Autowired
    private VoucherTemplateRepository templateRepository;

    @Autowired
    private VoucherBatchRepository batchRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherUserService voucherUserService;

    @Autowired
    private VoucherCoreService voucherCoreService;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        voucherRepository.deleteAll();
        batchRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Test
    void testFullVoucherLifecycle() throws Exception {
        String orgId = "TEST-ORG";
        String rawCode = "TEST-12345";
        
        // 1. Setup metadata
        VoucherTemplate template = new VoucherTemplate();
        template.setName("Test Template");
        template.setCodeFormat("TEST-#####");
        template.setOrgId(orgId);
        template = templateRepository.save(template);

        VoucherBatch batch = new VoucherBatch();
        batch.setOrgId(orgId);
        batch.setTemplateId(template.getId());
        batch.setQuantity(1);
        batch.setStatus(VoucherBatch.BatchStatus.ACTIVE);
        batch.setCreatedDate(LocalDateTime.now());
        batch = batchRepository.save(batch);

        // 2. Manually create a voucher to simulate outcome of generation
        Voucher voucher = new Voucher();
        voucher.setOrgId(orgId);
        voucher.setBatchId(batch.getId());
        voucher.setEncryptedCode(encryptionService.encrypt(rawCode));
        voucher.setCodeHash(voucherCoreService.hash(rawCode));
        voucher.setMaxUsage(1);
        voucher = voucherRepository.save(voucher);

        // 3. Validate Voucher via API
        com.voucher.user.controller.VoucherController.ValidateRequest validateRequest = new com.voucher.user.controller.VoucherController.ValidateRequest();
        validateRequest.setCode(rawCode);

        mockMvc.perform(post("/api/v1/vouchers/validate")
                .with(jwt().jwt(j -> j.claim("orgId", orgId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 4. Redeem Voucher
        com.voucher.user.controller.VoucherController.RedeemRequest redeemRequest = new com.voucher.user.controller.VoucherController.RedeemRequest();
        redeemRequest.setCode(rawCode);
        redeemRequest.setUserId("test-user");

        mockMvc.perform(post("/api/v1/vouchers/redeem")
                .with(jwt().jwt(j -> j.claim("orgId", orgId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(redeemRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usageCount").value(1))
                .andExpect(jsonPath("$.status").value("REDEEMED"));
    }
}
