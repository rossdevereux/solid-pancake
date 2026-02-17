package com.voucher.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voucher.admin.service.BatchService;
import com.voucher.core.domain.VoucherBatch;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BatchControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private BatchService batchService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testGetAllBatches() throws Exception {
        VoucherBatch batch = new VoucherBatch();
        batch.setId("batch-1");
        
        when(batchService.getAllBatches()).thenReturn(Collections.singletonList(batch));

        mockMvc.perform(get("/api/v1/batches")
                .with(jwt().jwt(j -> j.claim("orgId", "TEST-ORG"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("batch-1"));
    }

    @Test
    void testCreateBatch() throws Exception {
        VoucherBatch batch = new VoucherBatch();
        batch.setId("batch-new");
        batch.setStatus(VoucherBatch.BatchStatus.GENERATING);

        when(batchService.createBatch(anyString(), anyInt())).thenReturn(batch);

        BatchController.CreateBatchRequest request = new BatchController.CreateBatchRequest();
        request.setTemplateId("tmpl-1");
        request.setCount(100);

        mockMvc.perform(post("/api/v1/batches")
                .with(jwt().jwt(j -> j.claim("orgId", "TEST-ORG")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("batch-new"))
                .andExpect(jsonPath("$.status").value("GENERATING"));
    }
}
