package com.voucher.admin.service;

import com.voucher.core.domain.Voucher;
import com.voucher.core.domain.VoucherBatch;
import com.voucher.core.domain.VoucherTemplate;
import com.voucher.core.repository.VoucherRepository;
import com.voucher.core.service.EncryptionService;
import com.voucher.core.service.VoucherCoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherAdminServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private VoucherCoreService voucherCoreService;

    @InjectMocks
    private VoucherAdminService voucherAdminService;

    @Test
    @SuppressWarnings("unchecked")
    void testGenerateVouchersSuccess() {
        VoucherBatch batch = new VoucherBatch();
        batch.setId("batch-1");
        batch.setOrgId("ORG-1");

        VoucherTemplate template = new VoucherTemplate();
        template.setId("tmpl-1");
        template.setCodeFormat("TEST-####");

        when(voucherCoreService.generateCode(anyString())).thenReturn("TEST-1234");
        when(voucherCoreService.hash(anyString())).thenReturn("hash-1234");
        when(encryptionService.encrypt(anyString())).thenReturn("enc-1234");

        // Mock Redis pipeline
        // executePipelined returns a List of results from the Redis operations
        // If sAdd returns 1, it means the element was added (new)
        when(redisTemplate.executePipelined(any(RedisCallback.class)))
                .thenReturn(Collections.singletonList(1L));

        int generated = voucherAdminService.generateVouchers(batch, template, 1);

        assertEquals(1, generated);
        verify(voucherRepository, times(1)).saveAll(anyList());
        verify(voucherCoreService, atLeastOnce()).generateCode(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGenerateVouchersWithCollision() {
        VoucherBatch batch = new VoucherBatch();
        batch.setId("batch-1");
        batch.setOrgId("ORG-1");

        VoucherTemplate template = new VoucherTemplate();
        template.setId("tmpl-1");
        template.setCodeFormat("TEST-####");

        when(voucherCoreService.generateCode(anyString())).thenReturn("COLLISION", "NEW-CODE");
        when(voucherCoreService.hash(anyString())).thenReturn("hash-collision", "hash-new");
        when(encryptionService.encrypt(anyString())).thenReturn("enc-coll", "enc-new");

        // First call return 0 (collision), second call return 1 (success)
        when(redisTemplate.executePipelined(any(RedisCallback.class)))
                .thenReturn(Collections.singletonList(0L))
                .thenReturn(Collections.singletonList(1L));

        int generated = voucherAdminService.generateVouchers(batch, template, 1);

        assertEquals(1, generated);
        verify(voucherRepository, times(1)).saveAll(anyList());
        verify(redisTemplate, times(2)).executePipelined(any(RedisCallback.class));
    }
}
