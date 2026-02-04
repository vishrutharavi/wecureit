package com.wecureit.service;

import java.util.*;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.wecureit.entity.Card;
import com.wecureit.entity.Patient;
import com.wecureit.repository.CardRepository;
import com.wecureit.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.wecureit.dto.request.CardRequest;
import com.wecureit.dto.response.CardResponse;
import com.wecureit.dto.response.CardDetailResponse;
import com.wecureit.utilities.CardEncryptionService;

@Service
public class CardService {

    private final CardRepository repo;
    private final CardEncryptionService encryptService;
    private final Logger log = LoggerFactory.getLogger(CardService.class);
    private final PatientRepository patientRepository;

    public CardService(CardRepository repo, CardEncryptionService encryptService, PatientRepository patientRepository) {
        this.repo = repo;
        this.encryptService = encryptService;
        this.patientRepository = patientRepository;
    }

    public Card addCard(CardRequest req) throws Exception {
        // fetch Patient and set the relation
        UUID patientId = req.getPatientMasterId();
        Patient pm = patientRepository.findById(patientId).orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        // Check for duplicate PAN for this patient by decrypting existing cards
        List<Card> existingCards = repo.findAllByPatientIdAndIsActiveTrueOrderByIdAsc(patientId);
        for (Card existing : existingCards) {
            try {
                String existingPan = encryptService.decryptCard(existing.getEncryptedPan(), existing.getWrappedDek(), existing.getIv());
                if (existingPan != null && existingPan.equals(req.getPan())) {
                    throw new Exception("Card with same PAN already exists for this patient");
                }
            } catch (Exception ex) {
                // If decryption of an existing card fails (for example because the
                // master key used to wrap the DEK changed), log and skip that card.
                log.warn("Could not decrypt existing card id={} last4={}: {}", existing.getId(), existing.getLast4(), ex.getMessage());
                System.err.println("Warning: could not decrypt existing card id=" + existing.getId() + ": " + ex.getMessage());
                // continue to next card
            }
        }

        // Encrypt PAN, CVC and expiry together so they share same wrapped DEK
        String expiryStr = String.format("%02d/%04d", req.getExpMonth(), req.getExpYear());
        var multi = encryptService.encryptMultiple(req.getPan(), req.getCvc(), expiryStr);
        String[] encryptedVals = multi.encryptedValues();
        String[] ivs = multi.ivs();

        Card card = new Card();
        card.setEncryptedPan(encryptedVals.length > 0 ? encryptedVals[0] : null);
        card.setEncryptedCvc(encryptedVals.length > 1 ? encryptedVals[1] : null);
        card.setEncryptedExpiry(encryptedVals.length > 2 ? encryptedVals[2] : null);
        card.setWrappedDek(multi.wrappedDek());
        // store IVs per-field
        card.setIv(ivs.length > 0 ? ivs[0] : null);
        card.setCvcIv(ivs.length > 1 ? ivs[1] : null);
        card.setExpiryIv(ivs.length > 2 ? ivs[2] : null);
        card.setLast4(req.getPan().substring(req.getPan().length() - 4));
        card.setPatient(pm);
        return repo.save(card);
    }

    public String viewCard(Long id) throws Exception {
        Card card = repo.findById(id).orElseThrow();
        return encryptService.decryptCard(card.getEncryptedPan(), card.getWrappedDek(), card.getIv());
    }

    public CardDetailResponse getCardDetail(Long id) throws Exception {
        Card card = repo.findById(id).orElseThrow();
        String pan = encryptService.decryptCard(card.getEncryptedPan(), card.getWrappedDek(), card.getIv());
        Integer expMonth = null;
        Integer expYear = null;
        try {
            if (card.getEncryptedExpiry() != null && card.getExpiryIv() != null) {
                String expiry = encryptService.decryptCard(card.getEncryptedExpiry(), card.getWrappedDek(), card.getExpiryIv());
                // Expecting MM/YYYY or M/YYYY
                var parts = expiry.split("/");
                if (parts.length >= 2) {
                    expMonth = Integer.parseInt(parts[0]);
                    expYear = Integer.parseInt(parts[1]);
                }
            }
        } catch (Exception e) {
            log.warn("Could not decrypt expiry for card id={}: {}", card.getId(), e.getMessage());
        }
        return new CardDetailResponse(card.getId(), pan, card.getLast4(), expMonth, expYear);
    }

    public Card updateCard(Long id, CardRequest req) throws Exception {
        Card card = repo.findById(id).orElseThrow();
        // If PAN provided, treat as replacement and re-encrypt fields
        if (req.getPan() != null && !req.getPan().isBlank()) {
            // Duplicate check excluding current card
            List<Card> existingCards = repo.findAllByPatientIdAndIsActiveTrueOrderByIdAsc(req.getPatientMasterId());
            for (Card existing : existingCards) {
                if (existing.getId().equals(id)) continue;
                try {
                    String existingPan = encryptService.decryptCard(existing.getEncryptedPan(), existing.getWrappedDek(), existing.getIv());
                    if (existingPan != null && existingPan.equals(req.getPan())) {
                        throw new Exception("Card with same PAN already exists for this patient");
                    }
                } catch (Exception ex) {
                    System.err.println("Warning: could not decrypt existing card id=" + existing.getId() + ": " + ex.getMessage());
                }
            }

            String expiryStr = String.format("%02d/%04d", req.getExpMonth(), req.getExpYear());
            var multi = encryptService.encryptMultiple(req.getPan(), req.getCvc(), expiryStr);
            String[] encryptedVals = multi.encryptedValues();
            String[] ivs = multi.ivs();
            card.setEncryptedPan(encryptedVals.length > 0 ? encryptedVals[0] : null);
            card.setEncryptedCvc(encryptedVals.length > 1 ? encryptedVals[1] : null);
            card.setWrappedDek(multi.wrappedDek());
            card.setIv(ivs.length > 0 ? ivs[0] : null);
            card.setCvcIv(ivs.length > 1 ? ivs[1] : null);
            card.setLast4(req.getPan().substring(req.getPan().length() - 4));
            card.setEncryptedExpiry(encryptedVals.length > 2 ? encryptedVals[2] : null);
            card.setExpiryIv(ivs.length > 2 ? ivs[2] : null);
        }
        // Update expiry even if PAN not changed
        // If expiry fields provided, re-encrypt and update encryptedExpiry
        try {
            String expiryStr = String.format("%02d/%04d", req.getExpMonth(), req.getExpYear());
            var enc = encryptService.encryptMultiple(expiryStr);
            // encryptMultiple expects at least one value; it will generate its own wrapped DEK.
            // We will store the encrypted expiry and its IV. Note: this creates a new wrappedDek
            // -- acceptable for replacing expiry on update.
            card.setEncryptedExpiry(enc.encryptedValues().length > 0 ? enc.encryptedValues()[0] : null);
            card.setExpiryIv(enc.ivs().length > 0 ? enc.ivs()[0] : null);
            card.setWrappedDek(enc.wrappedDek());
        } catch (Exception ex) {
            log.warn("Could not encrypt updated expiry for card id={}: {}", card.getId(), ex.getMessage());
        }
        return repo.save(card);
    }

    public List<CardResponse> getCardsByPatientId(UUID patientMasterId) {
        // prefer returning id+last4 so the client can manage cards (delete/edit)
        try {
            return repo.findCardResponsesByPatientIdOrderByIdAsc(patientMasterId);
        } catch (Exception ex) {
            // fallback to legacy last4-only list mapped to CardResponse with id=null
            var legacy = repo.findByPatientIdOrderByIdAsc(patientMasterId);
            var out = new ArrayList<CardResponse>();
            for (String s : legacy) out.add(new CardResponse(null, s));
            return out;
        }
    }

    public void deleteCard(Long id, UUID patientMasterId) {
        var opt = repo.findById(id);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Card not found");
        }
        Card card = opt.get();
        if (patientMasterId != null) {
            Patient pm = card.getPatient();
            if (pm == null || !pm.getId().equals(patientMasterId)) {
                throw new IllegalArgumentException("Card not found for patient");
            }
        }
        // Soft-delete: mark as inactive so we retain encrypted data but hide from UI
        card.setIsActive(Boolean.FALSE);
        repo.save(card);
    }
}
