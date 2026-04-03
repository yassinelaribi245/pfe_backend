package tn.star.star_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.star.star_api.dto.OfferPaymentResponse;
import tn.star.star_api.entity.Offer;
import tn.star.star_api.service.PaymentService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ── GET /api/offers/{id}/payments ─────────────────────────────────────
    // Returns all registrants with their payment installments for this offer
    // Accessible by: admin, super_admin, association_member of that category
    @GetMapping("/api/offers/{id}/payments")
    public ResponseEntity<?> getOfferPayments(
            @PathVariable UUID id,
            @RequestParam(required = false) String method,
            Authentication auth) {
        List<OfferPaymentResponse> payments =
            paymentService.getPaymentsForOffer(id, auth.getName(), method);
        return ResponseEntity.ok(payments);
    }

    // ── PATCH /api/payments/{id}/pay ──────────────────────────────────────
    // Member marks a specific installment as paid
    @PatchMapping("/api/payments/{id}/pay")
    public ResponseEntity<?> markPaid(
            @PathVariable UUID id,
            Authentication auth) {
        return ResponseEntity.ok(
            paymentService.markPaid(id, auth.getName()));
    }

    // ── PATCH /api/payments/{id}/overdue ──────────────────────────────────
    // Member marks a specific installment as overdue
    @PatchMapping("/api/payments/{id}/overdue")
    public ResponseEntity<?> markOverdue(
            @PathVariable UUID id,
            Authentication auth) {
        paymentService.markOverdue(id);
        return ResponseEntity.ok(
            Map.of("message", "Versement marqué comme en retard"));
    }

    // ── POST /api/offers/{id}/payments/generate ───────────────────────────
    // Manually trigger payment generation when offer closes
    // Called by admin/member when they close the offer
    @PostMapping("/api/offers/{id}/payments/generate")
    public ResponseEntity<?> generatePayments(
            @PathVariable UUID id,
            Authentication auth) {
        paymentService.generatePaymentsForOffer(id);
        return ResponseEntity.ok(
            Map.of("message", "Paiements générés avec succès"));
    }

    // ── PATCH /api/registrations/{id}/approve ────────────────────────────
    // Member approves a pending_approval registration
    @PatchMapping("/api/registrations/{id}/approve")
    public ResponseEntity<?> approve(
            @PathVariable UUID id,
            Authentication auth) {
        paymentService.approvePendingRegistration(id, auth.getName());
        return ResponseEntity.ok(
            Map.of("message", "Inscription approuvée"));
    }

    // ── PATCH /api/registrations/{id}/refuse ─────────────────────────────
    // Member refuses a pending_approval registration
    @PatchMapping("/api/registrations/{id}/refuse")
    public ResponseEntity<?> refuse(
            @PathVariable UUID id,
            Authentication auth) {
        paymentService.refusePendingRegistration(id, auth.getName());
        return ResponseEntity.ok(
            Map.of("message", "Inscription refusée"));
    }

    // ── GET /api/offers/payment-methods ──────────────────────────────────
    // Returns all available payment method options for the create offer form
    @GetMapping("/api/offers/payment-methods")
    public ResponseEntity<?> getPaymentMethods() {
        List<Map<String, String>> methods = List.of(
            Map.of("value", Offer.PaymentMethod.free.name(),     "label", Offer.PaymentMethod.free.label()),
            Map.of("value", Offer.PaymentMethod.full.name(),     "label", Offer.PaymentMethod.full.label()),
            Map.of("value", Offer.PaymentMethod.months_3.name(), "label", Offer.PaymentMethod.months_3.label()),
            Map.of("value", Offer.PaymentMethod.months_6.name(), "label", Offer.PaymentMethod.months_6.label()),
            Map.of("value", Offer.PaymentMethod.months_9.name(), "label", Offer.PaymentMethod.months_9.label()),
            Map.of("value", Offer.PaymentMethod.months_12.name(),"label", Offer.PaymentMethod.months_12.label())
        );
        return ResponseEntity.ok(methods);
    }
}
