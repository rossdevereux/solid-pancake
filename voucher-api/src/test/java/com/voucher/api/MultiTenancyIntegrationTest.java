package com.voucher.api;

import com.voucher.api.config.TenantContext;
import com.voucher.api.repository.VoucherTemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest
class MultiTenancyIntegrationTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private VoucherTemplateRepository templateRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    void testDataIsolationBetweenTenants() throws Exception {
        // Create template for Org A
        mockMvc.perform(post("/api/v1/templates")
                .with(jwt().jwt(j -> j.claim("orgId", "ORG-A")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Template A\", \"codeFormat\": \"AAAA-####\"}"))
                .andExpect(status().isOk());

        // Create template for Org B
        mockMvc.perform(post("/api/v1/templates")
                .with(jwt().jwt(j -> j.claim("orgId", "ORG-B")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Template B\", \"codeFormat\": \"BBBB-####\"}"))
                .andExpect(status().isOk());

        // Verify Org A only sees Template A
        mockMvc.perform(get("/api/v1/templates")
                .with(jwt().jwt(j -> j.claim("orgId", "ORG-A"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Template A"));

        // Verify Org B only sees Template B
        mockMvc.perform(get("/api/v1/templates")
                .with(jwt().jwt(j -> j.claim("orgId", "ORG-B"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Template B"));
    }
}
