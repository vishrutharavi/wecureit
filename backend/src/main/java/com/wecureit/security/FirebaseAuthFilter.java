package com.wecureit.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.wecureit.repository.AdminRepository;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.repository.PatientRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public FirebaseAuthFilter(AdminRepository adminRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // Skip filter for:
        // 1. Auth endpoints
        // 2. Agent endpoints (health and ask)
        // 3. Health check endpoints
        // 4. OPTIONS requests (CORS preflight)
        return uri.startsWith("/api/auth/") || 
               uri.equals("/api/agent/health") ||
               uri.equals("/api/agent/ask") ||
               uri.startsWith("/api/agent/booking/") ||
               uri.startsWith("/api/health") ||
               uri.startsWith("/health") ||
               uri.startsWith("/actuator") ||
               "OPTIONS".equalsIgnoreCase(method);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            String msg = "Missing or invalid Authorization header";
            System.out.println("FirebaseAuthFilter - rejecting request " + request.getRequestURI() + " : " + msg);
            response.getWriter().write("{\"error\":\"" + msg + "\"}");
            return;
        }

        String token = authHeader.substring(7);

        try {
            FirebaseToken decodedToken =
                FirebaseAuth.getInstance().verifyIdToken(token);

            // debug: print UID and full claims map to help diagnose authorization issues
            System.out.println("FirebaseAuthFilter - decoded UID: " + decodedToken.getUid());
            System.out.println("FirebaseAuthFilter - claims: " + decodedToken.getClaims());

            String role = null;
            String roleSource = null;
            Object claim = decodedToken.getClaims().get("role");
            if (claim != null) {
                role = claim.toString().trim();
                roleSource = "claim";
            }

            // Fallback: if claim missing, try DB lookup by firebase uid
            if (role == null || role.isBlank()) {
                String uid = decodedToken.getUid();
                // check admin
                if (adminRepository.findByFirebaseUid(uid).isPresent()) {
                    role = "ADMIN";
                    roleSource = "db-uid";
                } else if (doctorRepository.findByFirebaseUid(uid).isPresent()) {
                    role = "DOCTOR";
                    roleSource = "db-uid";
                } else if (patientRepository.findByFirebaseUid(uid).isPresent()) {
                    role = "PATIENT";
                    roleSource = "db-uid";
                }
            }

            // Additional fallback: if still missing, try resolving by email present in the token
            if (role == null || role.isBlank()) {
                String email = decodedToken.getEmail();
                if (email != null && !email.isBlank()) {
                    // check admin by email
                    if (adminRepository.findByEmail(email).isPresent()) {
                        role = "ADMIN";
                        roleSource = "db-email";
                    } else if (doctorRepository.findByEmail(email).isPresent()) {
                        role = "DOCTOR";
                        roleSource = "db-email";
                    } else if (patientRepository.findByEmail(email).isPresent()) {
                        role = "PATIENT";
                        roleSource = "db-email";
                    }
                }
            }

            if (role == null || role.isBlank()) {
                String uid = decodedToken.getUid();
                String msg = "No role claim present and no DB mapping for uid=" + uid;
                System.out.println("FirebaseAuthFilter - " + msg + " requestUri=" + request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"" + msg + "\"}");
                return;
            }

            role = role.toUpperCase();

            // debug logging - remove or replace with proper logger in production
            System.out.println("FirebaseAuthFilter - uid=" + decodedToken.getUid() + " role=" + role + " (source=" + roleSource + ")");

            List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
            );

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    decodedToken.getUid(),
                    null,
                    authorities
                );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            String msg = "Token verification failed: " + e.getMessage();
            System.out.println("FirebaseAuthFilter - token verification failed for request " + request.getRequestURI() + " : " + e.getMessage());
            response.getWriter().write("{\"error\":\"" + msg.replaceAll("\"", "\\\"") + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
