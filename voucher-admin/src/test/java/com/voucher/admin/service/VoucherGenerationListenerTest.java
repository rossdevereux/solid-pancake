package com.voucher.admin.service;

import com.voucher.core.domain.VoucherBatch;
import com.voucher.core.domain.VoucherTemplate;
import com.voucher.core.dto.VoucherGenerationTask;
import com.voucher.core.repository.VoucherBatchRepository;
import com.voucher.core.repository.VoucherTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherGenerationListenerTest {

    @Mock
    private VoucherAdminService voucherService;

    @Mock
    private VoucherBatchRepository batchRepository;

    @Mock
    private VoucherTemplateRepository templateRepository;

    @InjectMocks
    private VoucherGenerationListener listener;

    @Test
    void testHandleGenerationSuccess() {
        VoucherGenerationTask task = new VoucherGenerationTask("batch-1", "tmpl-1", "ORG-1");
        
        VoucherBatch batch = new VoucherBatch();
        batch.setId("batch-1");
        batch.setQuantity(5);
        batch.setGeneratedCount(0);
        
        VoucherTemplate template = new VoucherTemplate();
        template.setId("tmpl-1");

        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch));
        when(templateRepository.findById("tmpl-1")).thenReturn(Optional.of(template));
        
        // Mock 1st chunk generation
        when(voucherService.generateVouchers(any(), any(), anyInt())).thenReturn(5);

        listener.handleGeneration(task);

        assertEquals(5, batch.getGeneratedCount());
        assertEquals(VoucherBatch.BatchStatus.ACTIVE, batch.getStatus());
        verify(batchRepository, atLeastOnce()).save(batch);
    }

    @Test
    void testHandleGenerationFailureExhausted() {
        VoucherGenerationTask task = new VoucherGenerationTask("batch-1", "tmpl-1", "ORG-1");
        
        VoucherBatch batch = new VoucherBatch();
        batch.setId("batch-1");
        batch.setQuantity(100);
        batch.setGeneratedCount(0);
        
        VoucherTemplate template = new VoucherTemplate();
        template.setId("tmpl-1");

        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch));
        when(templateRepository.findById("tmpl-1")).thenReturn(Optional.of(template));
        
        // Fail to generate any vouchers (exhausted pattern)
        when(voucherService.generateVouchers(any(), any(), anyInt())).thenReturn(0);

        listener.handleGeneration(task);

        assertEquals(VoucherBatch.BatchStatus.PENDING, batch.getStatus());
        verify(batchRepository, atLeastOnce()).save(batch);
    }
}
