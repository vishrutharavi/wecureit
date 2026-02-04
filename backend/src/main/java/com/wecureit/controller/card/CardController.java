package com.wecureit.controller.card;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.request.CardRequest;
import com.wecureit.dto.response.CardResponse;
import com.wecureit.entity.Card;
import com.wecureit.service.CardService;
@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;
    private final Logger log = LoggerFactory.getLogger(CardController.class);

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/add")
    public ResponseEntity<CardResponse> addCard(@RequestBody CardRequest req) throws Exception {
        Card card = cardService.addCard(req);
        URI location = URI.create("/cards/" + card.getId());
        // return only id and last4 in response (see CardResponse)
        return ResponseEntity.created(location).body(new CardResponse(card.getId(), card.getLast4()));
    }

    @GetMapping("/{id}")
    public String viewCard(@PathVariable Long id) throws Exception {
        return cardService.viewCard(id);
    }

    @GetMapping("/{id}/masked")
    public String viewMaskedCard(@PathVariable Long id) throws Exception {
        String pan = cardService.viewCard(id);
        return "**** **** **** " + pan.substring(pan.length() - 4);
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<?> viewCardDetail(@PathVariable Long id) {
        try {
            var detail = cardService.getCardDetail(id);
            return ResponseEntity.ok(detail);
        } catch (NoSuchElementException | IllegalArgumentException notFound) {
            // Card not found
            return ResponseEntity.status(404).body(Map.of("error", "Card not found"));
        } catch (Exception ex) {
            // Decryption or KMS failure — do not return stack trace to client
            log.warn("Failed to reveal card detail for id={}: {}", id, ex.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "Unable to reveal card details. Try again or contact support."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCard(@PathVariable Long id, @RequestBody CardRequest req) {
        // Card updates are temporarily disabled while we resolve KMS/decryption issues.
        log.info("Rejected attempt to update card id={}: card updates are disabled", id);
        return ResponseEntity.status(405).body(Map.of("error", "Card update temporarily disabled"));
    }

    @GetMapping("/getcards")
    public List<CardResponse> getCards(@RequestParam(name = "patientId") UUID patientMasterId) {
        log.info("GET /cards/getcards called for patientId={}", patientMasterId);
        var cards = cardService.getCardsByPatientId(patientMasterId);
        log.info("Returning {} cards for patientId={}", cards == null ? 0 : cards.size(), patientMasterId);
        return cards;
    }

    // Delete a card by id. Optional patientId used for authorization checks (if implemented)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id, @RequestParam(name = "patientId", required = false) UUID patientMasterId) {
        try {
            cardService.deleteCard(id, patientMasterId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(404).build();
        } catch (Exception ex) {
            return ResponseEntity.status(500).build();
        }
    }


}
