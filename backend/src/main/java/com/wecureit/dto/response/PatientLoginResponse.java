package com.wecureit.dto.response;


public class PatientLoginResponse {

    private String firebaseUid;
    private String email;

    public PatientLoginResponse(String firebaseUid, String email) {
        this.firebaseUid = firebaseUid;
        this.email = email;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getEmail() {
        return email;
    }
}
