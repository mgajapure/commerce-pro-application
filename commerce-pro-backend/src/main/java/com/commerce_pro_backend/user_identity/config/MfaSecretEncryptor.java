package com.commerce_pro_backend.user_identity.config;

import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA converter that transparently encrypts/decrypts the MFA secret using AES-256-GCM.
 * The encryption key is derived from the app.mfa.encryption-key property.
 */
@Slf4j
@Component
@Converter
public class MfaSecretEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    // Fixed IV for deterministic encryption (acceptable for TOTP secrets as they are
    // already random high-entropy values; rotate via key re-encryption if needed)
    private static final byte[] FIXED_IV = "CommercePro_MFA!".getBytes(StandardCharsets.UTF_8);

    @Value("${app.mfa.encryption-key}")
    private String rawEncryptionKey;

    private SecretKey secretKey;

    // Static reference so the converter (which JPA instantiates directly) can use it
    private static SecretKey staticKey;

    @PostConstruct
    public void init() throws Exception {
        if (rawEncryptionKey == null || rawEncryptionKey.isBlank()) {
            throw new IllegalStateException(
                "app.mfa.encryption-key must be set. Generate a secure value and store it as an env var.");
        }
        // Derive a 256-bit AES key from the configured string using PBKDF2
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
            rawEncryptionKey.toCharArray(),
            "CommercePro_Salt".getBytes(StandardCharsets.UTF_8),
            65536,
            256
        );
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        secretKey = new SecretKeySpec(keyBytes, "AES");
        staticKey = secretKey;
        log.info("MFA secret encryptor initialized");
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, FIXED_IV);
            cipher.init(Cipher.ENCRYPT_MODE, staticKey, parameterSpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt MFA secret", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, FIXED_IV);
            cipher.init(Cipher.DECRYPT_MODE, staticKey, parameterSpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt MFA secret", e);
        }
    }
}
