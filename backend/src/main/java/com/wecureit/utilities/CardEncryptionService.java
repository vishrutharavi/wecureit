package com.wecureit.utilities;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.springframework.stereotype.Service;

@Service
public class CardEncryptionService {

    private final LocalKmsService kmsService;
    private final SecureRandom random = new SecureRandom();

    public CardEncryptionService(LocalKmsService kmsService) {
        this.kmsService = kmsService;
    }

    public EncryptedCard encryptCard(String plainPan) throws Exception {
        // Generate DEK
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey dek = keyGen.generateKey();

        // Encrypt PAN with DEK
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        random.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, dek, new GCMParameterSpec(128, iv));
        byte[] cipherText = cipher.doFinal(plainPan.getBytes(StandardCharsets.UTF_8));

        // Wrap DEK using master key
        byte[] wrappedDek = kmsService.wrapKey(dek);
        Arrays.fill(plainPan.toCharArray(), '\0'); // clear sensitive data

        return new EncryptedCard(
            Base64.getEncoder().encodeToString(cipherText),
            Base64.getEncoder().encodeToString(wrappedDek),
            Base64.getEncoder().encodeToString(iv)
        );
    }

    public String decryptCard(String encPan, String wrappedDek, String iv) throws Exception {
        SecretKey dek = kmsService.unwrapKey(Base64.getDecoder().decode(wrappedDek));
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, dek, new GCMParameterSpec(128, Base64.getDecoder().decode(iv)));
        byte[] plainBytes = cipher.doFinal(Base64.getDecoder().decode(encPan));
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    /**
     * Encrypt multiple plain values using a single DEK and IV and return all encrypted
     * values plus the wrapped DEK and IV (all Base64 encoded).
     * Note: this keeps the original pattern of using one wrapped DEK and one IV per card.
     */
    public EncryptedMultiple encryptMultiple(String... plainValues) throws Exception {
        // Generate DEK
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey dek = keyGen.generateKey();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        String[] encrypted = new String[plainValues.length];
        String[] ivs = new String[plainValues.length];
        for (int i = 0; i < plainValues.length; i++) {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, dek, new GCMParameterSpec(128, iv));
            byte[] cipherText = cipher.doFinal(plainValues[i].getBytes(StandardCharsets.UTF_8));
            encrypted[i] = Base64.getEncoder().encodeToString(cipherText);
            ivs[i] = Base64.getEncoder().encodeToString(iv);
            Arrays.fill(plainValues[i].toCharArray(), '\0'); // clear sensitive data
        }

        // Wrap DEK using master key
        byte[] wrappedDek = kmsService.wrapKey(dek);

        return new EncryptedMultiple(encrypted, Base64.getEncoder().encodeToString(wrappedDek), ivs);
    }

    public record EncryptedMultiple(String[] encryptedValues, String wrappedDek, String[] ivs) {}

    public record EncryptedCard(String encryptedPan, String wrappedDek, String iv) {}
}

