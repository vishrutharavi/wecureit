package com.wecureit.dto.request;

public class UpdateDoctorRequest {
	private String name;
	private String gender;

	public UpdateDoctorRequest() {}

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getGender() { return gender; }
	public void setGender(String gender) { this.gender = gender; }
}
