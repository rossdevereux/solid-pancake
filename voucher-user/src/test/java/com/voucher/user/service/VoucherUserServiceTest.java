package com.voucher.user.service;

import com.voucher.core.domain.Voucher;
import com.voucher.core.repository.VoucherRepository;
import com.voucher.core.service.VoucherCoreService;
import com.voucher.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class VoucherUserServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private VoucherCoreService voucherCoreService;

    @Mock
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @InjectMocks
    private VoucherUserService voucherUserService;

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
        voucher.setMaxUsage(1);

        when(voucherCoreService.hash(anyString())).thenReturn("hashed-code");
        when(voucherRepository.findByCodeHashAndOrgId(anyString(), any())).thenReturn(Optional.of(voucher));

        Voucher result = voucherUserService.validate(code);

        assertNotNull(result);
        assertEquals(Voucher.VoucherStatus.ACTIVE, result.getStatus());
        verify(voucherRepository).findByCodeHashAndOrgId(eq("hashed-code"), any());
    }

    @Test
    void testValidate_NotFound() {
        when(voucherCoreService.hash(anyString())).thenReturn("hashed-code");
        when(voucherRepository.findByCodeHashAndOrgId(anyString(), any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> voucherUserService.validate("INVALID-CODE"));
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

        when(voucherCoreService.hash(anyString())).thenReturn("hashed-code");
        when(voucherRepository.findByCodeHashAndOrgId(anyString(), any())).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(i -> i.getArguments()[0]);

        Voucher result = voucherUserService.redeem(code, userId);

        assertNotNull(result);
        assertEquals(1, result.getUsageCount());
        assertEquals(Voucher.VoucherStatus.REDEEMED, result.getStatus());
        assertEquals(1, result.getRedemptions().size());
        assertEquals(userId, result.getRedemptions().get(0).getUserId());
    }
}
