package com.voucher.api;

import tools.jackson.databind.ObjectMapper;
import com.voucher.api.controller.VoucherController;
import com.voucher.api.domain.Voucher;
import com.voucher.api.domain.VoucherBatch;
import com.voucher.api.domain.VoucherTemplate;
import com.voucher.api.repository.VoucherBatchRepository;
import com.voucher.api.repository.VoucherRepository;
import com.voucher.api.repository.VoucherTemplateRepository;
import com.voucher.api.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VoucherIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VoucherTemplateRepository templateRepository;

    @Autowired
    private VoucherBatchRepository batchRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.voucher.api.service.EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        voucherRepository.deleteAll();
        batchRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Test
    void testFullVoucherLifecycle() throws Exception {
        // 1. Create Template
        VoucherTemplate template = new VoucherTemplate();
        template.setName("Test Template");
        template.setCodeFormat("TEST-####");
        template = templateRepository.save(template);

        // 2. Create Batch & Generate Vouchers
        VoucherBatch batch = new VoucherBatch();
        batch.setTemplateId(template.getId());
        batch.setQuantity(10);
        batch.setStatus(VoucherBatch.BatchStatus.GENERATING);
        batch.setCreatedDate(LocalDateTime.now());
        batch = batchRepository.save(batch);

        voucherService.generateVouchers(batch, template, batch.getQuantity());

        // Verify generation
        List<Voucher> vouchers = voucherRepository.findAll();
        assertEquals(10, vouchers.size());

        // 3. Validate Voucher via API
        String encryptedCode = vouchers.get(0).getEncryptedCode();
        String rawCode = encryptionService.decrypt(encryptedCode);

        VoucherController.ValidateRequest validateRequest = new VoucherController.ValidateRequest();
        validateRequest.setCode(rawCode);

        mockMvc.perform(post("/api/v1/vouchers/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // 4. Redeem Voucher
        VoucherController.RedeemRequest redeemRequest = new VoucherController.RedeemRequest();
        redeemRequest.setCode(rawCode);
        redeemRequest.setUserId("test-user");

        mockMvc.perform(post("/api/v1/vouchers/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(redeemRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usageCount").value(1))
                .andExpect(jsonPath("$.status").value("REDEEMED"));
    }
}
