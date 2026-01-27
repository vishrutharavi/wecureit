package com.wecureit.service.patient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.wecureit.controller.AuthController;
import com.wecureit.dto.request.SignupRequest;
import com.wecureit.service.AuthService;

class PatientRegisterControllerTest {

    static class FakeAuthService extends AuthService {
        boolean called = false;

        public FakeAuthService() {
            super(null, null, null);
        }

        @Override
        public void signup(SignupRequest request) {
            this.called = true;
        }
    }

    @Test
    void controller_calls_service_on_signup() {
        FakeAuthService fake = new FakeAuthService();
        AuthController controller = new AuthController(fake);

        SignupRequest req = new SignupRequest();
        req.setEmail("henry@example.com");
        req.setPassword("Password123!");
        req.setRole("PATIENT");
        req.setName("Henry");
        req.setPhone("4673887628");
        req.setDob(java.time.LocalDate.parse("2003-02-05"));
        req.setGender("MALE");
        req.setCity("Alexandria");
        req.setState("Virginia");
        req.setZip("22305");

    ResponseEntity<?> resp = controller.signupPatient(req);
    Assertions.assertTrue(resp.getStatusCode().is2xxSuccessful());
        Assertions.assertTrue(fake.called, "AuthService.signup should have been called");
    }
}
