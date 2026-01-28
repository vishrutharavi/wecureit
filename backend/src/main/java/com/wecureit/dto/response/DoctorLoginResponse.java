package com.wecureit.dto.response;


public class DoctorLoginResponse {
    private String id;
    private String firebaseUid;
    private String email;
    private String name;
    private String gender;

    public DoctorLoginResponse(String id, String firebaseUid, String email, String name, String gender) {
        this.id = id;
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.name = name;
        this.gender = gender;
    }

    public String getId() {
        return id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }
}