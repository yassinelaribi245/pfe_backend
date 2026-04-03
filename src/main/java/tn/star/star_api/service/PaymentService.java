package tn.star.star_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.star.star_api.dto.OfferPaymentResponse;
import tn.star.star_api.entity.*;
import tn.star.star_api.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OfferPaymentRepository      paymentRepository;
    private final OfferRegistrationRepository registrationRepository;
    private final OfferRepository             offerRepository;
    private final UserRepository              userRepository;
    private final NotificationRepository      notificationRepository;

    // ── Generate payments when an offer closes ────────────────────────────
    // Called when: offer fills up OR admin marks it finished
    @Transactional
    public void generatePaymentsForOffer(UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
            .orElseThrow(() -> new RuntimeException("Offre introuvable"));

        // Only generate for paid methods
        if (offer.getPaymentMethod() == Offer.PaymentMethod.free) return;

        List<OfferRegistration> confirmed =
            registrationRepository.findConfirmedByOfferId(offerId);

        for (OfferRegistration reg : confirmed) {
            // Skip if payments already generated for this registration
            if (!paymentRepository.findByRegistrationId(reg.getId()).isEmpty()) continue;
            generateInstallments(reg, offer);
        }
    }

    // ── Generate installments for one registration ────────────────────────
    private void generateInstallments(OfferRegistration reg, Offer offer) {
        int n = offer.getPaymentMethod().installments();
        BigDecimal total = offer.getPrice();
        BigDecimal installmentAmount = total.divide(
            BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);

        LocalDate startDate = offer.getEndDate() != null
            ? offer.getEndDate() : LocalDate.now();

        for (int i = 1; i <= n; i++) {
            OfferPayment payment = new OfferPayment();
            payment.setRegistration(reg);
            payment.setInstallmentNumber(i);
            payment.setTotalInstallments(n);
            payment.setAmount(installmentAmount);
            // Each installment due one month apart starting from offer end
            payment.setDueDate(startDate.plusMonths(i - 1));
            payment.setStatus(OfferPayment.PaymentStatus.pending);
            paymentRepository.save(payment);
        }
    }

    // ── Mark an installment as paid ───────────────────────────────────────
    @Transactional
    public OfferPaymentResponse.Installment markPaid(UUID paymentId, String memberEmail) {
        OfferPayment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Paiement introuvable"));

        User member = userRepository.findByEmail(memberEmail)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        payment.setStatus(OfferPayment.PaymentStatus.paid);
        payment.setPaidAt(java.time.OffsetDateTime.now());
        payment.setMarkedBy(member);
        OfferPayment saved = paymentRepository.save(payment);

        // Notify the user their payment was recorded
        Notification notif = new Notification();
        notif.setUser(payment.getRegistration().getUser());
        notif.setTitle("💳 Paiement enregistré");
        notif.setBody("Votre versement n°" + payment.getInstallmentNumber()
            + "/" + payment.getTotalInstallments()
            + " pour l'offre \""
            + payment.getRegistration().getOffer().getTitle()
            + "\" a été marqué comme payé.");
        notif.setType(Notification.NotifType.payment);
        notificationRepository.save(notif);

        return new OfferPaymentResponse.Installment(
            saved.getId(),
            saved.getInstallmentNumber(),
            saved.getTotalInstallments(),
            saved.getAmount(),
            saved.getDueDate(),
            saved.getPaidAt(),
            member.getName() + " " + member.getLastName(),
            saved.getStatus().name()
        );
    }

    // ── Mark an installment as overdue ────────────────────────────────────
    @Transactional
    public void markOverdue(UUID paymentId) {
        OfferPayment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Paiement introuvable"));
        if (payment.getStatus() == OfferPayment.PaymentStatus.pending) {
            payment.setStatus(OfferPayment.PaymentStatus.overdue);
            paymentRepository.save(payment);

            // Notify the user their payment is overdue
            Notification notif = new Notification();
            notif.setUser(payment.getRegistration().getUser());
            notif.setTitle("⚠️ Paiement en retard");
            notif.setBody("Votre versement n°"
                + payment.getInstallmentNumber()
                + "/" + payment.getTotalInstallments()
                + " pour l'offre \""
                + payment.getRegistration().getOffer().getTitle()
                + "\" est marqué en retard. Veuillez régulariser votre situation.");
            notif.setType(Notification.NotifType.payment);
            notificationRepository.save(notif);
        }
    }

    // ── Get all payment records for an offer ──────────────────────────────
    public List<OfferPaymentResponse> getPaymentsForOffer(UUID offerId, String email, String filterMethod) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Offer offer = offerRepository.findById(offerId)
            .orElseThrow(() -> new RuntimeException("Offre introuvable"));

        // All registrations for this offer (confirmed + pending_approval)
        List<OfferRegistration> registrations =
            registrationRepository.findByOfferId(offerId)
                .stream()
                .filter(r -> r.getStatus() != OfferRegistration.RegistrationStatus.cancelled
                          && r.getStatus() != OfferRegistration.RegistrationStatus.waitlist)
                .filter(r -> filterMethod == null || filterMethod.isEmpty()
                          || filterMethod.equals(r.getChosenPaymentMethod()))
                .collect(Collectors.toList());

        // All payments for this offer
        List<OfferPayment> allPayments = paymentRepository.findByOfferId(offerId);
        Map<UUID, List<OfferPayment>> byReg = allPayments.stream()
            .collect(Collectors.groupingBy(p -> p.getRegistration().getId()));

        return registrations.stream()
            .map(reg -> OfferPaymentResponse.from(
                reg,
                byReg.getOrDefault(reg.getId(), Collections.emptyList())
            ))
            .collect(Collectors.toList());
    }

    // ── Approve a pending_approval registration ───────────────────────────
    @Transactional
    public void approvePendingRegistration(UUID registrationId, String memberEmail) {
        OfferRegistration reg = registrationRepository.findById(registrationId)
            .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        if (reg.getStatus() != OfferRegistration.RegistrationStatus.pending_approval) {
            throw new RuntimeException("Cette inscription n'est pas en attente d'approbation");
        }

        Offer offer = reg.getOffer();

        // Count current confirmed registrations
        long confirmed = registrationRepository.findByOfferId(offer.getId())
            .stream()
            .filter(r -> r.getStatus() == OfferRegistration.RegistrationStatus.confirmed)
            .count();

        // Check capacity (0 = unlimited)
        if (offer.getMaxParticipants() > 0 && confirmed >= offer.getMaxParticipants()) {
            // Offer is full — put on waitlist instead
            reg.setStatus(OfferRegistration.RegistrationStatus.waitlist);
            registrationRepository.save(reg);
            notifyUser(reg.getUser(), offer,
                "⏳ Inscription sur liste d'attente",
                "L'offre \"" + offer.getTitle() + "\" est complète. "
                + "Vous avez été placé(e) sur liste d'attente.");
        } else {
            reg.setStatus(OfferRegistration.RegistrationStatus.confirmed);
            registrationRepository.save(reg);
            notifyUser(reg.getUser(), offer,
                "✅ Inscription approuvée",
                "Votre demande d'inscription à l'offre \""
                + offer.getTitle() + "\" a été approuvée par le responsable.");
        }
    }

    // ── Refuse a pending_approval registration ────────────────────────────
    @Transactional
    public void refusePendingRegistration(UUID registrationId, String memberEmail) {
        OfferRegistration reg = registrationRepository.findById(registrationId)
            .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        if (reg.getStatus() != OfferRegistration.RegistrationStatus.pending_approval) {
            throw new RuntimeException("Cette inscription n'est pas en attente d'approbation");
        }

        reg.setStatus(OfferRegistration.RegistrationStatus.cancelled);
        registrationRepository.save(reg);

        notifyUser(reg.getUser(), reg.getOffer(),
            "❌ Inscription refusée",
            "Votre demande d'inscription à l'offre \""
            + reg.getOffer().getTitle()
            + "\" a été refusée par le responsable en raison d'un paiement en retard.");
    }

    // ── Helper — send notification to user ───────────────────────────────
    private void notifyUser(User user, Offer offer, String title, String body) {
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setTitle(title);
        notif.setBody(body);
        notif.setType(Notification.NotifType.payment);
        notificationRepository.save(notif);
    }
}
