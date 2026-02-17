package com.voucher.admin;

import com.voucher.core.config.RabbitStatsConfig;
import com.voucher.core.config.TenantContext;
import com.voucher.core.domain.DailyStats;
import com.voucher.core.domain.GlobalStats;
import com.voucher.core.domain.VoucherStatsMessage;
import com.voucher.core.repository.VoucherStatsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate; 
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class VoucherStatsIntegrationTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private VoucherStatsRepository statsRepository; // This is the custom one I created

    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Clear existing stats
        mongoTemplate.dropCollection(DailyStats.class);
        mongoTemplate.dropCollection(GlobalStats.class);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testStatsFlow() throws Exception {
        String orgId = "STATS-ORG";
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 1. Simulate Voucher Generation
        int generatedCount = 10;
        VoucherStatsMessage genMsg = VoucherStatsMessage.builder()
                .orgId(orgId)
                .type(VoucherStatsMessage.StatsType.GENERATION)
                .count(generatedCount)
                .date(today)
                .build();
        
        rabbitTemplate.convertAndSend(RabbitStatsConfig.VOUCHER_STATS_EXCHANGE, RabbitStatsConfig.VOUCHER_STATS_ROUTING_KEY, genMsg);

        // Wait for GlobalStats update
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            return statsRepository.findGlobalStatsByOrgId(orgId)
                    .map(s -> s.getTotalVouchers() == generatedCount)
                    .orElse(false);
        });

        // 2. Simulate Voucher Redemption
        int redeemedCount = 2;
        for (int i = 0; i < redeemedCount; i++) {
             VoucherStatsMessage redMsg = VoucherStatsMessage.builder()
                .orgId(orgId)
                .type(VoucherStatsMessage.StatsType.REDEMPTION)
                .count(1)
                .date(today)
                .build();
             rabbitTemplate.convertAndSend(RabbitStatsConfig.VOUCHER_STATS_EXCHANGE, RabbitStatsConfig.VOUCHER_STATS_ROUTING_KEY, redMsg);
        }

        // Wait for stats to reflect redemption
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
             return statsRepository.getRedemptionsToday(orgId, today) == redeemedCount;
        });

        // 3. Verify via API
        mockMvc.perform(get("/api/v1/stats")
                .with(jwt().jwt(j -> j.claim("orgId", orgId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVouchers", is(generatedCount)))
                .andExpect(jsonPath("$.activeVouchers", is(generatedCount - redeemedCount))) // Active = Total - Redeemed (simplified logic for this test)
                // Note: The GlobalStats logic I implemented: 
                // incrementGenerationStats: totalVouchers += count, activeVouchers += count
                // incrementRedemptionStats: redeemedVouchers += 1, activeVouchers -= 1
                // So Active should be 10 - 2 = 8.
                .andExpect(jsonPath("$.redeemedVouchers", is(redeemedCount)))
                .andExpect(jsonPath("$.redemptionsToday", is(redeemedCount)));
    }
}
