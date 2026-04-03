package tn.star.star_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.star.star_api.service.OfferRegistrationService;
import java.util.UUID;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferRegistrationController {

    private final OfferRegistrationService registrationService;

    // POST /api/offers/{id}/register
    // Any logged-in user registers to an offer
    @PostMapping("/{id}/register")
    public ResponseEntity<?> register(@PathVariable UUID id,
                                      Authentication auth,
                                      @RequestBody(required = false)
                                          java.util.Map<String, String> body) {
        String chosenMethod = body != null ? body.get("chosenPaymentMethod") : null;
        return ResponseEntity.ok(
            registrationService.register(id, auth.getName(), chosenMethod));
    }

    // DELETE /api/offers/{id}/register
    // Any user cancels their own registration
    @DeleteMapping("/{id}/register")
    public ResponseEntity<?> cancelRegistration(@PathVariable UUID id,
                                                Authentication auth) {
        registrationService.cancelRegistration(id, auth.getName());
        return ResponseEntity.ok("Inscription annulée");
    }

    // GET /api/offers/registrations/mine
    // Any user — see all offers I registered to
    @GetMapping("/registrations/mine")
    public ResponseEntity<?> getMyRegistrations(Authentication auth) {
        return ResponseEntity.ok(
            registrationService.getMyRegistrations(auth.getName()));
    }

    // GET /api/offers/{id}/registrations
    // Member → only if offer is in their category
    // Admin  → any offer they choose
    @GetMapping("/{id}/registrations")
    public ResponseEntity<?> getOfferRegistrations(@PathVariable UUID id,
                                                   Authentication auth) {
        return ResponseEntity.ok(
            registrationService.getOfferRegistrations(id, auth.getName()));
    }
}
