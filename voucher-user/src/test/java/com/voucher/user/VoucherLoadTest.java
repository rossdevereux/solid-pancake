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
import com.voucher.user.controller.VoucherController;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class VoucherLoadTest {

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
    private EncryptionService encryptionService;

    @Autowired
    private VoucherCoreService voucherCoreService;

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
    void testConcurrentRedemption() throws Exception {
        int voucherCount = 100;
        int threads = 10;
        String orgId = "LOAD-ORG";

        // 1. Setup Data - Manually prepopulate for redemption test
        VoucherTemplate template = new VoucherTemplate();
        template.setName("Load Test Template");
        template.setCodeFormat("LOAD-#####");
        template.setOrgId(orgId);
        template = templateRepository.save(template);

        VoucherBatch batch = new VoucherBatch();
        batch.setOrgId(orgId);
        batch.setTemplateId(template.getId());
        batch.setQuantity(voucherCount);
        batch.setStatus(VoucherBatch.BatchStatus.ACTIVE);
        batch.setCreatedDate(LocalDateTime.now());
        batch = batchRepository.save(batch);

        List<Voucher> vouchersToSave = new ArrayList<>();
        List<String> rawCodes = new ArrayList<>();
        for (int i = 0; i < voucherCount; i++) {
            String code = "LOAD-" + String.format("%05d", i);
            rawCodes.add(code);
            Voucher v = new Voucher();
            v.setOrgId(orgId);
            v.setBatchId(batch.getId());
            v.setEncryptedCode(encryptionService.encrypt(code));
            v.setCodeHash(voucherCoreService.hash(code));
            v.setMaxUsage(1);
            vouchersToSave.add(v);
        }
        voucherRepository.saveAll(vouchersToSave);

        // 2. Prepare for concurrent redemption
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (String code : rawCodes) {
            executor.submit(() -> {
                try {
                    VoucherController.RedeemRequest request = new VoucherController.RedeemRequest();
                    request.setCode(code);
                    request.setUserId("user-" + System.nanoTime());

                    mockMvc.perform(post("/api/v1/vouchers/redeem")
                            .with(jwt().jwt(j -> j.claim("orgId", orgId)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(finished, "Test timed out");
        assertEquals(voucherCount, successCount.get(), "All vouchers should be redeemed successfully");
        assertEquals(0, failCount.get());

        System.out.println("Redeemed " + voucherCount + " vouchers in " + duration + "ms");
    }
}
