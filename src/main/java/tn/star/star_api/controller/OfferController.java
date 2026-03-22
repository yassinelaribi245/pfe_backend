package tn.star.star_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.star.star_api.dto.OfferRequest;
import tn.star.star_api.service.OfferService;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @GetMapping("/mine")
    public ResponseEntity<?> getMyOffers(Authentication auth) {
        return ResponseEntity.ok(offerService.getMyOffers(auth.getName()));
    }

    @GetMapping
    public ResponseEntity<?> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(offerService.getOffersByStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(offerService.getOfferById(id));
    }

    @PostMapping
    public ResponseEntity<?> createOffer(Authentication auth,
            @Valid @RequestBody OfferRequest req) {
        return ResponseEntity.ok(
            offerService.createOffer(auth.getName(), req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOffer(@PathVariable UUID id,
            Authentication auth,
            @Valid @RequestBody OfferRequest req) {
        return ResponseEntity.ok(
            offerService.updateOffer(id, auth.getName(), req));
    }

    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<?> changeStatus(@PathVariable UUID id,
            @PathVariable String status,
            Authentication auth) {
        return ResponseEntity.ok(
            offerService.changeStatus(id, status, auth.getName()));
    }

    // DELETE — pass authenticated user so it gets logged correctly
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOffer(@PathVariable UUID id,
            Authentication auth) {
        offerService.deleteOffer(id, auth.getName());
        return ResponseEntity.ok(
            Map.of("message", "Offre supprimée"));
    }
}
