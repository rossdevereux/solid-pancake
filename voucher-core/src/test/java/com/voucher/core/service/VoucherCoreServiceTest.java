package com.voucher.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoucherCoreServiceTest {

    private VoucherCoreService voucherCoreService;

    @BeforeEach
    void setUp() {
        voucherCoreService = new VoucherCoreService();
    }

    @Test
    void testHash() {
        String code = "TEST-CODE-123";
        String hash1 = voucherCoreService.hash(code);
        String hash2 = voucherCoreService.hash(code);
        
        assertNotNull(hash1);
        assertEquals(hash1, hash2); // Deterministic
        assertNotEquals(code, hash1);
    }

    @Test
    void testGenerateCodeWithNumericPattern() {
        String pattern = "CODE-####";
        String code = voucherCoreService.generateCode(pattern);
        
        assertNotNull(code);
        assertEquals(10, code.length()); // "CODE-" (5) + "####" (4) + check digit (1)
        assertTrue(code.startsWith("CODE-"));
        assertTrue(code.substring(5, 9).matches("\\d{4}"));
    }

    @Test
    void testGenerateCodeWithAlphanumericPattern() {
        String pattern = "ABC-????";
        String code = voucherCoreService.generateCode(pattern);
        
        assertNotNull(code);
        assertEquals(9, code.length()); // "ABC-" (4) + "????" (4) + check digit (1)
        assertTrue(code.startsWith("ABC-"));
    }

    @Test
    void testGenerateCodeWithExplicitCheckDigit() {
        String pattern = "FIXED-###*";
        String code = voucherCoreService.generateCode(pattern);
        
        assertNotNull(code);
        assertEquals(10, code.length()); // "FIXED-" (6) + "###" (3) + "*" (1)
        assertTrue(code.startsWith("FIXED-"));
        // The '*' should have been replaced by the calculated check digit
        assertFalse(code.contains("*"));
    }

    @Test
    void testCheckDigitLogic() {
        String data = "VOUCHER123";
        char checkDigit = voucherCoreService.calculateCheckDigit(data);
        
        String fullCode = data + checkDigit;
        assertTrue(voucherCoreService.validateCheckDigit(fullCode));
        
        // Mutate and ensure it fails
        String invalidCode = data + (checkDigit == 'A' ? 'B' : 'A');
        assertFalse(voucherCoreService.validateCheckDigit(invalidCode));
    }

    @Test
    void testValidateCheckDigitEdgeCases() {
        assertFalse(voucherCoreService.validateCheckDigit(null));
        assertFalse(voucherCoreService.validateCheckDigit("A")); // Too short
        assertFalse(voucherCoreService.validateCheckDigit("!!!")); // No valid chars
    }
}
