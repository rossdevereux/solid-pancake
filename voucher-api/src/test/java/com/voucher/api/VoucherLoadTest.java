package com.voucher.api;

import tools.jackson.databind.ObjectMapper;
import com.voucher.api.controller.VoucherController;
import com.voucher.api.domain.Voucher;
import com.voucher.api.domain.VoucherBatch;
import com.voucher.api.domain.VoucherTemplate;
import com.voucher.api.repository.VoucherBatchRepository;
import com.voucher.api.repository.VoucherRepository;
import com.voucher.api.repository.VoucherTemplateRepository;
import com.voucher.api.service.EncryptionService;
import com.voucher.api.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VoucherLoadTest {

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
    private EncryptionService encryptionService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        voucherRepository.deleteAll();
        batchRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Test
    void testConcurrentRedemption() throws Exception {
        int voucherCount = 100;
        int threads = 10;

        // 1. Setup Data
        VoucherTemplate template = new VoucherTemplate();
        template.setName("Load Test Template");
        template.setCodeFormat("LOAD-#####");
        VoucherTemplate.RedemptionLimit limit = new VoucherTemplate.RedemptionLimit();
        limit.setLimitPerUser(1);
        template.setRedemptionLimit(limit);
        template = templateRepository.save(template);

        VoucherBatch batch = new VoucherBatch();
        batch.setTemplateId(template.getId());
        batch.setQuantity(voucherCount);
        batch.setStatus(VoucherBatch.BatchStatus.GENERATING);
        batch.setCreatedDate(LocalDateTime.now());
        batch = batchRepository.save(batch);

        voucherService.generateVouchers(batch, template, voucherCount);

        List<Voucher> vouchers = voucherRepository.findAll();
        assertEquals(voucherCount, vouchers.size());

        // 2. Prepare for concurrent redemption
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Use a synchronized list or just pick random vouchers?
        // Let's have multiple users try to redeem unique vouchers to simulate traffic
        List<String> rawCodes = new ArrayList<>();
        for (Voucher v : vouchers) {
            rawCodes.add(encryptionService.decrypt(v.getEncryptedCode()));
        }

        long startTime = System.currentTimeMillis();

        for (String code : rawCodes) {
            executor.submit(() -> {
                try {
                    VoucherController.RedeemRequest request = new VoucherController.RedeemRequest();
                    request.setCode(code);
                    request.setUserId("user-" + System.nanoTime()); // Unique user per redemption

                    mockMvc.perform(post("/api/v1/vouchers/redeem")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    e.printStackTrace();
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

    @Test
    void testBulkGenerationPerformance() {
        int quantity = 10000; // Testing local batch generation speed

        VoucherTemplate template = new VoucherTemplate();
        template.setName("Perf Test Template");
        template.setCodeFormat("PERF-#####");
        template = templateRepository.save(template);

        VoucherBatch batch = new VoucherBatch();
        batch.setTemplateId(template.getId());
        batch.setQuantity(quantity);
        batch.setStatus(VoucherBatch.BatchStatus.GENERATING);
        batch.setCreatedDate(LocalDateTime.now());
        batch = batchRepository.save(batch);

        long startTime = System.currentTimeMillis();
        voucherService.generateVouchers(batch, template, quantity);
        long duration = System.currentTimeMillis() - startTime;

        List<Voucher> vouchers = voucherRepository.findAll();
        assertEquals(quantity, vouchers.size());

        System.out.println("Generated " + quantity + " vouchers in " + duration + "ms");
        // Assert it's reasonably fast (e.g., > 100 vouchers/sec)
        // 2000 vouchers in < 5 seconds seems reasonable for local test with
        // mocked/local DB
        assertTrue(duration < 10000, "Generation took too long: " + duration + "ms");
    }
}
