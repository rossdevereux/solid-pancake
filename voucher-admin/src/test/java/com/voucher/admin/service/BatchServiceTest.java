package com.voucher.admin.service;

import com.voucher.admin.config.RabbitMQConfig;
import com.voucher.core.config.TenantContext;
import com.voucher.core.domain.VoucherBatch;
import com.voucher.core.domain.VoucherTemplate;
import com.voucher.core.dto.VoucherGenerationTask;
import com.voucher.core.exception.ResourceNotFoundException;
import com.voucher.core.exception.ValidationException;
import com.voucher.core.repository.VoucherBatchRepository;
import com.voucher.core.repository.VoucherTemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private VoucherBatchRepository batchRepository;

    @Mock
    private VoucherTemplateRepository templateRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private BatchService batchService;

    private static final String ORG_ID = "TEST-ORG";
    private static final String TEMPLATE_ID = "tmpl-1";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(ORG_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testCreateBatchSuccess() {
        VoucherTemplate template = new VoucherTemplate();
        template.setId(TEMPLATE_ID);
        template.setOrgId(ORG_ID);
        
        when(templateRepository.findByIdAndOrgId(TEMPLATE_ID, ORG_ID)).thenReturn(Optional.of(template));
        when(batchRepository.save(any(VoucherBatch.class))).thenAnswer(i -> {
            VoucherBatch b = i.getArgument(0);
            b.setId("batch-1");
            return b;
        });

        VoucherBatch result = batchService.createBatch(TEMPLATE_ID, 100);

        assertNotNull(result);
        assertEquals("batch-1", result.getId());
        assertEquals(VoucherBatch.BatchStatus.GENERATING, result.getStatus());
        
        ArgumentCaptor<VoucherGenerationTask> taskCaptor = ArgumentCaptor.forClass(VoucherGenerationTask.class);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.VOUCHER_GEN_EXCHANGE),
            eq(RabbitMQConfig.VOUCHER_GEN_ROUTING_KEY),
            taskCaptor.capture()
        );
        
        VoucherGenerationTask task = taskCaptor.getValue();
        assertEquals("batch-1", task.getBatchId());
        assertEquals(TEMPLATE_ID, task.getTemplateId());
    }

    @Test
    void testCreateBatchTemplateNotFound() {
        when(templateRepository.findByIdAndOrgId(TEMPLATE_ID, ORG_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> batchService.createBatch(TEMPLATE_ID, 100));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void testUpdateStatusSuccess() {
        VoucherBatch batch = new VoucherBatch();
        batch.setId("batch-1");
        batch.setStatus(VoucherBatch.BatchStatus.PENDING);
        
        when(batchRepository.findByIdAndOrgId("batch-1", ORG_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(VoucherBatch.class))).thenAnswer(i -> i.getArgument(0));

        VoucherBatch result = batchService.updateStatus("batch-1", VoucherBatch.BatchStatus.ACTIVE);
        
        assertEquals(VoucherBatch.BatchStatus.ACTIVE, result.getStatus());
        verify(batchRepository).save(batch);
    }

    @Test
    void testUpdateStatusForbiddenGenerating() {
        assertThrows(ValidationException.class, () -> 
            batchService.updateStatus("batch-1", VoucherBatch.BatchStatus.GENERATING));
    }
}
