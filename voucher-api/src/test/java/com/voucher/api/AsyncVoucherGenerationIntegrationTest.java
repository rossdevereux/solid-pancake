package com.voucher.api;

import com.voucher.api.config.TenantContext;
import com.voucher.api.domain.VoucherBatch;
import com.voucher.api.domain.VoucherTemplate;
import com.voucher.api.repository.VoucherBatchRepository;
import com.voucher.api.repository.VoucherRepository;
import com.voucher.api.repository.VoucherTemplateRepository;
import org.junit.jupiter.api.AfterEach;
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

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AsyncVoucherGenerationIntegrationTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private VoucherTemplateRepository templateRepository;

    @Autowired
    private VoucherBatchRepository batchRepository;

    @Autowired
    private VoucherRepository voucherRepository;

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

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testAsyncVoucherGenerationFlow() throws Exception {
        String orgId = "TEST-ORG";
        int quantity = 50;

        // 1. Create Template
        VoucherTemplate template = new VoucherTemplate();
        template.setName("Async Test Template");
        template.setCodeFormat("ASYNC-####");
        template.setOrgId(orgId);
        template = templateRepository.save(template);

        // 2. Trigger Batch Generation via API
        String resultJson = mockMvc.perform(post("/api/v1/batches")
                .with(jwt().jwt(j -> j.claim("orgId", orgId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"templateId\": \"%s\", \"count\": %d}", template.getId(), quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GENERATING"))
                .andReturn().getResponse().getContentAsString();

        // Extract batch ID from response
        String tempId = resultJson.substring(resultJson.indexOf("\"id\":\"") + 6);
        final String finalBatchId = tempId.substring(0, tempId.indexOf("\""));

        // 3. Wait for Async Processing
        // Note: This requires a running RabbitMQ instance.
        // In a real CI environment, we'd use Testcontainers for RabbitMQ.
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            VoucherBatch batch = batchRepository.findById(finalBatchId).orElse(null);
            return batch != null && batch.getStatus() == VoucherBatch.BatchStatus.ACTIVE;
        });

        // 4. Verify Final State
        mockMvc.perform(get("/api/v1/batches/" + finalBatchId)
                .with(jwt().jwt(j -> j.claim("orgId", orgId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.generatedCount").value(quantity));

        // 5. Verify Vouchers are in DB
        long voucherCount = voucherRepository.countByOrgId(orgId);
        assertEquals(quantity, voucherCount, "Expected number of vouchers should be generated");
    }
}
