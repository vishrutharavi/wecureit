package com.wecureit.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.key-path:}")
    private String firebaseKeyPath;

    @PostConstruct
    public void init() throws Exception {
        String keyPath = (firebaseKeyPath == null || firebaseKeyPath.isBlank())
                ? System.getenv("FIREBASE_KEY_PATH")
                : firebaseKeyPath;

        if (keyPath == null || keyPath.isBlank()) {
            throw new RuntimeException("FIREBASE_KEY_PATH not set");
        }

        if (!Files.exists(Paths.get(keyPath))) {
            throw new RuntimeException("Firebase key file not found at: " + keyPath);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(
                        GoogleCredentials.fromStream(
                                new FileInputStream(keyPath)
                        )
                )
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}
