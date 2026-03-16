package tn.star.star_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.star.star_api.dto.OfferRequest;
import tn.star.star_api.service.OfferService;
import java.util.UUID;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    // GET /api/offers/mine  (association member — my offers only)
    @GetMapping("/mine")
    public ResponseEntity<?> getMyOffers(Authentication auth) {
        return ResponseEntity.ok(offerService.getMyOffers(auth.getName()));
    }

    // GET /api/offers
    @GetMapping
    public ResponseEntity<?> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    // GET /api/offers?status=active
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(offerService.getOffersByStatus(status));
    }

    // GET /api/offers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(offerService.getOfferById(id));
    }

    // POST /api/offers  (association member only)
    @PostMapping
    public ResponseEntity<?> createOffer(Authentication auth,
                                         @Valid @RequestBody OfferRequest req) {
        return ResponseEntity.ok(offerService.createOffer(auth.getName(), req));
    }

    // PUT /api/offers/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOffer(@PathVariable UUID id,
                                         Authentication auth,
                                         @Valid @RequestBody OfferRequest req) {
        return ResponseEntity.ok(offerService.updateOffer(id, auth.getName(), req));
    }

    // PATCH /api/offers/{id}/status/{status}  (admin only)
    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<?> changeStatus(@PathVariable UUID id,
                                          @PathVariable String status) {
        return ResponseEntity.ok(offerService.changeStatus(id, status));
    }

    // DELETE /api/offers/{id}  (admin only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOffer(@PathVariable UUID id) {
        offerService.deleteOffer(id);
        return ResponseEntity.ok("Offre supprimée");
    }
}
