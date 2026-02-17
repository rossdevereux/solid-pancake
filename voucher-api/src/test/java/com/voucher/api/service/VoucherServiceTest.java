package com.voucher.api.service;

import com.voucher.api.domain.Voucher;
import com.voucher.api.exception.ValidationException;
import com.voucher.api.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private VoucherService voucherService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testValidate_Success() {
        String code = "TEST-CODE";
        Voucher voucher = new Voucher();
        voucher.setStatus(Voucher.VoucherStatus.ACTIVE);
        voucher.setExpiryDate(LocalDateTime.now().plusDays(1));
        voucher.setUsageCount(0);

        // Fix: Use correct mock behavior. VoucherService validation hashes the code
        // internally.
        // But since hash() is private, we can't easily mock it directly in unit tests
        // unless we use PowerMock or verify the behavior.
        // However, the test should just mock repository return.

        // Important: VoucherService uses SHA-256 for hashing.
        // In a unit test, we might struggle to match the exact hash unless we replicate
        // logc.
        // But we can use any() for the hash argument in the repository find.

        when(voucherRepository.findByCodeHashAndOrgId(anyString(), anyString())).thenReturn(Optional.of(voucher));

        Voucher result = voucherService.validate(code);

        assertNotNull(result);
        assertEquals(Voucher.VoucherStatus.ACTIVE, result.getStatus());
        verify(voucherRepository).findByCodeHashAndOrgId(anyString(), anyString());
    }

    @Test
    void testValidate_NotFound() {
        when(voucherRepository.findByCodeHashAndOrgId(anyString(), anyString())).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> voucherService.validate("INVALID-CODE"));
    }

    @Test
    void testRedeem_Success() {
        String code = "TEST-CODE";
        String userId = "user1";
        Voucher voucher = new Voucher();
        voucher.setStatus(Voucher.VoucherStatus.ACTIVE);
        voucher.setExpiryDate(LocalDateTime.now().plusDays(1));
        voucher.setUsageCount(0);
        voucher.setMaxUsage(1);
        voucher.setRedemptions(new ArrayList<>());

        when(voucherRepository.findByCodeHashAndOrgId(anyString(), anyString())).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(i -> i.getArguments()[0]);

        Voucher result = voucherService.redeem(code, userId);

        assertNotNull(result);
        assertEquals(1, result.getUsageCount());
        assertEquals(Voucher.VoucherStatus.REDEEMED, result.getStatus());
        assertEquals(1, result.getRedemptions().size());
        assertEquals(userId, result.getRedemptions().get(0).getUserId());
    }

    // Since generateVouchers logic is heavily tied to Redis pipeline which is hard
    // to mock effectively
    // without complex setup, we will focus on validation/redemption logic here
    // or rely on integration tests for the full flow.
}
