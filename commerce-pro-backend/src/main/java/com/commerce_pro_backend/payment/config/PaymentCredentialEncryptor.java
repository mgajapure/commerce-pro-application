package com.commerce_pro_backend.payment.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * JPA converter that transparently encrypts/decrypts payment gateway credentials
 * using AES-256-GCM — the same pattern as MfaSecretEncryptor, but using a
 * separate key property so gateway keys are isolated from MFA key material.
 *
 * Property: app.payment.encryption-key
 */
@Slf4j
@Component
@Converter
public class PaymentCredentialEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final byte[] FIXED_IV = "CommercePay_GW!!".getBytes(StandardCharsets.UTF_8);

    @Value("${app.payment.encryption-key:CommercePro_PaymentKey_Dev_Only}")
    private String rawEncryptionKey;

    private static SecretKey staticKey;

    @PostConstruct
    public void init() throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
            rawEncryptionKey.toCharArray(),
            "CommercePro_PaySalt".getBytes(StandardCharsets.UTF_8),
            65536,
            256
        );
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        staticKey = new SecretKeySpec(keyBytes, "AES");
        log.info("Payment credential encryptor initialized");
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, staticKey, new GCMParameterSpec(GCM_TAG_LENGTH, FIXED_IV));
            return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt payment credential", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, staticKey, new GCMParameterSpec(GCM_TAG_LENGTH, FIXED_IV));
            return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt payment credential", e);
        }
    }
}
