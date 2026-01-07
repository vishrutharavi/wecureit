package com.wecureit.dto.response;

import java.util.UUID;

public class AuthResponse {

    private String firebaseUid;
    private String email;
    private String role;

    public AuthResponse(String firebaseUid, String email, String role) {
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.role = role;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}