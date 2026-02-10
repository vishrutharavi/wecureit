package com.wecureit.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * LocalKmsService securely loads the AES master key from the local keystore.
 * The keystore file is stored within the project (e.g., /keys/wecureit-keystore.jceks)
 * and used to wrap/unwrap per-card encryption keys.
 */
@Service
public class LocalKmsService {

    private final SecretKey masterKey;

    public LocalKmsService(
            @Value("${wecureit.keystore.path:}") String keystorePath,
            @Value("${wecureit.keystore.password:}") String keystorePassword,
            @Value("${wecureit.key.alias:wecureitMasterKey}") String keyAlias
    ) throws Exception {

        // If keystore properties are missing, fall back to a temporary in-memory AES key.
        // This is convenient for local development but NOT suitable for production because
        // wrapped keys will not survive application restarts. For production, set
        // 'wecureit.keystore.path' and 'wecureit.keystore.password' to a valid keystore.
        if (keystorePath == null || keystorePath.isBlank() || keystorePassword == null || keystorePassword.isBlank()) {
            // generate ephemeral AES key for development
            java.security.SecureRandom sr = new java.security.SecureRandom();
            javax.crypto.KeyGenerator kg = javax.crypto.KeyGenerator.getInstance("AES");
            try {
                kg.init(256, sr);
            } catch (Exception ex) {
                // fall back to default key size if 256 isn't available
                kg.init(128, sr);
            }
            masterKey = kg.generateKey();
            System.out.println("WARNING: LocalKmsService running in development fallback mode — using an in-memory AES key.\n"
                    + "This is insecure for production and wrapped keys will not persist across restarts.\n"
                    + "Set 'wecureit.keystore.path' and 'wecureit.keystore.password' in application properties to enable a persistent keystore.");
            return;
        }

        // Try loading the keystore from several locations in order of preference:
        // 1) As a file at the given path (absolute or relative to current working dir)
        // 2) As a classpath resource (support path with "classpath:...")
        KeyStore ks = KeyStore.getInstance("JCEKS");

        File keystoreFile = new File(keystorePath);
        boolean loaded = false;

        if (keystoreFile.exists()) {
            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                ks.load(fis, keystorePassword.toCharArray());
                loaded = true;
            }
        } else {
            // support explicit classpath: prefix
            String resourcePath = keystorePath.startsWith("classpath:")
                    ? keystorePath.substring("classpath:".length())
                    : keystorePath;

            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
            if (is != null) {
                try (InputStream ris = is) {
                    ks.load(ris, keystorePassword.toCharArray());
                    loaded = true;
                }
            }
        }

        if (!loaded) {
            throw new IllegalStateException("Keystore file not found. Tried file: "
                    + keystoreFile.getAbsolutePath()
                    + " and classpath resource: " + keystorePath
                    + "\nPlease set property 'wecureit.keystore.path' to an absolute path or a classpath resource (prefix with 'classpath:').");
        }

        // Load AES master key
        masterKey = (SecretKey) ks.getKey(keyAlias, keystorePassword.toCharArray());
        if (masterKey == null) {
            throw new IllegalStateException("No key found in keystore for alias: " + keyAlias);
        }

        System.out.println("LocalKmsService initialized using keystore: " + keystoreFile.getAbsolutePath());
    }

    /** Wraps (encrypts) a Data Encryption Key (DEK) using the master key. */
    public byte[] wrapKey(SecretKey dek) throws Exception {
        Cipher cipher = Cipher.getInstance("AESWrap");
        cipher.init(Cipher.WRAP_MODE, masterKey);
        return cipher.wrap(dek);
    }

    /** Unwraps (decrypts) a wrapped DEK using the master key. */
    public SecretKey unwrapKey(byte[] wrappedDek) throws Exception {
        Cipher cipher = Cipher.getInstance("AESWrap");
        cipher.init(Cipher.UNWRAP_MODE, masterKey);
        return (SecretKey) cipher.unwrap(wrappedDek, "AES", Cipher.SECRET_KEY);
    }
}
