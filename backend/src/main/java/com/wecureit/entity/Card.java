package com.wecureit.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encrypted_pan", columnDefinition = "TEXT")
    private String encryptedPan;

    @Column(name = "wrapped_dek", columnDefinition = "TEXT")
    private String wrappedDek;

    @Column(name = "iv", columnDefinition = "TEXT")
    private String iv;

    @Column(name = "encrypted_cvc", columnDefinition = "TEXT")
    private String encryptedCvc;

    @Column(name = "cvc_iv", columnDefinition = "TEXT")
    private String cvcIv;

    @Column(name = "last4")
    private String last4;

    @Column(name = "encrypted_expiry", columnDefinition = "TEXT")
    private String encryptedExpiry;

    @Column(name = "expiry_iv", columnDefinition = "TEXT")
    private String expiryIv;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    // Link to the existing Patient entity. Database column is patient_master_id (UUID FK)
    @ManyToOne
    @JoinColumn(name = "patient_master_id", referencedColumnName = "id")
    private Patient patient;
}
