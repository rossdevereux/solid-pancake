package com.voucher.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class VoucherCoreService {

    protected static final String CHAR_NUMERIC = "0123456789";
    protected static final String CHAR_ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    protected static final String CHAR_ALPHANUMERIC = CHAR_ALPHA + CHAR_NUMERIC;
    protected static final String ALPHABET = CHAR_NUMERIC + CHAR_ALPHA;

    public String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateCode(String pattern) {
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        boolean explicitCheckDigit = pattern.contains("*");

        for (char c : pattern.toCharArray()) {
            if (c == '#') {
                sb.append(CHAR_NUMERIC.charAt(random.nextInt(CHAR_NUMERIC.length())));
            } else if (c == '?') {
                sb.append(CHAR_ALPHANUMERIC.charAt(random.nextInt(CHAR_ALPHANUMERIC.length())));
            } else if (c == '*') {
                sb.append('*');
            } else {
                sb.append(c);
            }
        }

        String code = sb.toString();
        char checkDigit = calculateCheckDigit(code.replace("*", ""));

        if (explicitCheckDigit) {
            return code.replace('*', checkDigit);
        } else {
            return code + checkDigit;
        }
    }

    public char calculateCheckDigit(String input) {
        String cleanInput = input.toUpperCase().replaceAll("[^0-9A-Z]", "");
        int sum = 0;
        int n = ALPHABET.length();

        for (int i = 0; i < cleanInput.length(); i++) {
            int val = ALPHABET.indexOf(cleanInput.charAt(i));
            int factor = ((cleanInput.length() - i) % 2 == 0) ? 2 : 1;
            int add = val * factor;
            sum += (add / n) + (add % n);
        }

        int remainder = sum % n;
        int checkIdx = (n - remainder) % n;
        return ALPHABET.charAt(checkIdx);
    }

    public boolean validateCheckDigit(String code) {
        if (code == null || code.length() < 2)
            return false;
        String cleanCode = code.toUpperCase().replaceAll("[^0-9A-Z]", "");
        if (cleanCode.length() < 2)
            return false;

        char provided = cleanCode.charAt(cleanCode.length() - 1);
        String data = cleanCode.substring(0, cleanCode.length() - 1);
        return calculateCheckDigit(data) == provided;
    }
}
