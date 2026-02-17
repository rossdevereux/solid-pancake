package com.voucher.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voucher.core.domain.VoucherTemplate;
import com.voucher.core.repository.VoucherTemplateRepository;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TemplateControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private VoucherTemplateRepository templateRepository;

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
    void testGetAllTemplates() throws Exception {
        VoucherTemplate template = new VoucherTemplate();
        template.setName("Test Template");
        
        when(templateRepository.findByOrgId("TEST-ORG")).thenReturn(Collections.singletonList(template));

        mockMvc.perform(get("/api/v1/templates")
                .with(jwt().jwt(j -> j.claim("orgId", "TEST-ORG"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Template"));
    }

    @Test
    void testCreateTemplate() throws Exception {
        VoucherTemplate template = new VoucherTemplate();
        template.setName("New Template");
        template.setCodeFormat("ABC-####");

        when(templateRepository.save(any(VoucherTemplate.class))).thenReturn(template);

        mockMvc.perform(post("/api/v1/templates")
                .with(jwt().jwt(j -> j.claim("orgId", "TEST-ORG")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(template)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Template"));
    }

    @Test
    void testGetTemplateNotFound() throws Exception {
        when(templateRepository.findByIdAndOrgId(anyString(), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/templates/non-existent")
                .with(jwt().jwt(j -> j.claim("orgId", "TEST-ORG"))))
                .andExpect(status().isNotFound());
    }
}
