package com.voucher.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;
    private final String testKey = "12345678901234567890123456789012"; // 32 bytes

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "secretKey", testKey);
        encryptionService.init();
    }

    @Test
    void testEncryptionDecryption() {
        String originalText = "Hello World!";
        String encrypted = encryptionService.encrypt(originalText);
        
        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void testEncryptionRandomness() {
        String text = "Stability Test";
        String encrypted1 = encryptionService.encrypt(text);
        String encrypted2 = encryptionService.encrypt(text);
        
        // Due to random IV, encrypted strings should be different
        assertNotEquals(encrypted1, encrypted2);
        
        // Both should decrypt to the same result
        assertEquals(text, encryptionService.decrypt(encrypted1));
        assertEquals(text, encryptionService.decrypt(encrypted2));
    }

    @Test
    void testInvalidKeyLength() {
        EncryptionService service = new EncryptionService();
        ReflectionTestUtils.setField(service, "secretKey", "short-key"); // Not 16/24/32 bytes
        
        assertThrows(IllegalArgumentException.class, service::init);
    }

    @Test
    void testDecryptionFailure() {
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt("invalid-base64"));
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt("YWJjZA==")); // Valid base64 but too short for IV
    }
}
