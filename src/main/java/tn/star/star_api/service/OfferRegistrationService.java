package tn.star.star_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.star.star_api.dto.OfferRegistrationResponse;
import tn.star.star_api.entity.*;
import tn.star.star_api.repository.*;
import tn.star.star_api.service.PaymentService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferRegistrationService {

    private final OfferRegistrationRepository registrationRepository;
    private final OfferRepository             offerRepository;
    private final UserRepository              userRepository;
    private final AssociationMemberRepository memberRepository;
    private final NotificationRepository      notificationRepository;
    private final OfferPaymentRepository      paymentRepository;
    private final ActivityLogService          logService;

    // ── Register to an offer (any role) ──────────────────
    @Transactional
    public OfferRegistrationResponse register(UUID offerId, String email, String chosenPaymentMethod) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));

        // Check offer allows registration
        boolean isFermer = offer.getStatus() == Offer.OfferStatus.fermer;
        if (offer.getStatus() != Offer.OfferStatus.active && !isFermer) {
            throw new RuntimeException(
                "Impossible de s'inscrire — l'offre n'est plus ouverte");
        }

        // Check if user already has a registration
        List<OfferRegistration> existing =
            registrationRepository.findByOfferId(offerId)
                .stream()
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .toList();

        if (!existing.isEmpty()) {
            OfferRegistration prev = existing.get(0);

            if (prev.getStatus() == OfferRegistration.RegistrationStatus.confirmed) {
                throw new RuntimeException("Vous êtes déjà inscrit à cette offre");
            }
            if (prev.getStatus() == OfferRegistration.RegistrationStatus.waitlist) {
                throw new RuntimeException("Vous êtes déjà sur liste d'attente");
            }

            // Was cancelled — try to re-register
            long confirmed = countConfirmed(offerId);

            if (offer.getMaxParticipants() > 0
                    && confirmed >= offer.getMaxParticipants()) {
                // Full — put back on waitlist
                prev.setStatus(OfferRegistration.RegistrationStatus.waitlist);
                registrationRepository.save(prev);
                logService.log(ActivityLogService.OFFER_WAITLIST, user,
                    "Offre \"" + offer.getTitle() + "\" (complète)");
                return OfferRegistrationResponse.from(prev);
            }

            // Space available — confirm directly
            prev.setChosenPaymentMethod(chosenPaymentMethod);
            prev.setStatus(OfferRegistration.RegistrationStatus.confirmed);
            OfferRegistrationResponse res =
                OfferRegistrationResponse.from(registrationRepository.save(prev));
            logService.log(ActivityLogService.OFFER_REGISTERED, user,
                "Offre \"" + offer.getTitle() + "\" (ré-inscription confirmée)");
            return res;
        }

        // New registration
        long confirmed = countConfirmed(offerId);
        OfferRegistration reg = new OfferRegistration();
        reg.setUser(user);
        reg.setOffer(offer);
        reg.setChosenPaymentMethod(chosenPaymentMethod);

        // If offer is fermer → straight to waitlist regardless of capacity
        if (isFermer) {
            reg.setStatus(OfferRegistration.RegistrationStatus.waitlist);
            registrationRepository.save(reg);
            logService.log(ActivityLogService.OFFER_WAITLIST, user,
                "Offre \"" + offer.getTitle() + "\" (fermée — liste d'attente)");
            return OfferRegistrationResponse.from(reg);
        }

        // Check if user has overdue payments — route to pending_approval
        boolean hasOverdue = paymentRepository.userHasOverduePayments(user.getId());
        if (hasOverdue) {
            reg.setStatus(OfferRegistration.RegistrationStatus.pending_approval);
            registrationRepository.save(reg);

            // Notify the responsible member
            notifyMemberPendingApproval(offer, user);

            logService.log(ActivityLogService.OFFER_REGISTERED, user,
                "Offre \"" + offer.getTitle() + "\" (en attente — paiement en retard)");
            return OfferRegistrationResponse.from(reg);
        }

        if (offer.getMaxParticipants() > 0
                && confirmed >= offer.getMaxParticipants()) {
            // Offer full — add to waitlist
            reg.setStatus(OfferRegistration.RegistrationStatus.waitlist);
            registrationRepository.save(reg);
            logService.log(ActivityLogService.OFFER_WAITLIST, user,
                "Offre \"" + offer.getTitle() + "\" (complète — liste d'attente)");
            return OfferRegistrationResponse.from(reg);
        }

        // Space available — confirm directly
        reg.setStatus(OfferRegistration.RegistrationStatus.confirmed);
        OfferRegistrationResponse result =
            OfferRegistrationResponse.from(registrationRepository.save(reg));
        logService.log(ActivityLogService.OFFER_REGISTERED, user,
            "Offre \"" + offer.getTitle() + "\"");

        // Check if offer just became full → set to fermer automatically
        long newConfirmed = countConfirmed(offerId);
        if (offer.getMaxParticipants() > 0
                && newConfirmed >= offer.getMaxParticipants()) {
            offer.setStatus(Offer.OfferStatus.fermer);
            offerRepository.save(offer);
            logService.log(ActivityLogService.OFFER_STATUS, user,
                "Offre \"" + offer.getTitle() + "\" : active → fermer (places complètes)");
        }
        return result;
    }

    // ── Cancel registration ───────────────────────────────
    @Transactional
    public void cancelRegistration(UUID offerId, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));

        OfferRegistration reg = registrationRepository.findByOfferId(offerId)
                .stream()
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                    "Vous n'êtes pas inscrit à cette offre"));

        if (reg.getStatus() == OfferRegistration.RegistrationStatus.cancelled) {
            throw new RuntimeException("Inscription déjà annulée");
        }

        boolean wasConfirmed =
            reg.getStatus() == OfferRegistration.RegistrationStatus.confirmed;

        // Cancel this registration
        reg.setStatus(OfferRegistration.RegistrationStatus.cancelled);
        registrationRepository.save(reg);
        logService.log(ActivityLogService.OFFER_CANCELLED, user,
            "Offre \"" + offer.getTitle() + "\"");

        // If the cancelled spot was confirmed → promote first waitlisted person
        if (wasConfirmed) {
            promoteFromWaitlist(offer);
        }
    }

    // ── Promote first person on waitlist ──────────────────
    private void promoteFromWaitlist(Offer offer) {
        // Get all waitlist registrations ordered by date (oldest first)
        registrationRepository.findByOfferId(offer.getId())
                .stream()
                .filter(r -> r.getStatus() ==
                    OfferRegistration.RegistrationStatus.waitlist)
                .min((a, b) -> a.getDate().compareTo(b.getDate()))
                .ifPresent(first -> {
                    // Promote to confirmed
                    first.setStatus(OfferRegistration.RegistrationStatus.confirmed);
                    registrationRepository.save(first);

                    // Send notification to the promoted user
                    Notification notif = new Notification();
                    notif.setUser(first.getUser());
                    notif.setTitle("🎉 Vous avez une place confirmée !");
                    notif.setBody("Votre inscription à l'offre \""
                        + offer.getTitle()
                        + "\" a été confirmée. Vous étiez sur liste d'attente.");
                    notif.setType(Notification.NotifType.offer);
                    notif.setIsRead(false);
                    notificationRepository.save(notif);
                });
    }

    // ── Notify member of pending approval request ────────
    private void notifyMemberPendingApproval(Offer offer, User user) {
        // Find the responsible member for this offer's category
        if (offer.getCreatedBy() == null) return;
        User memberUser = offer.getCreatedBy().getUser();
        Notification notif = new Notification();
        notif.setUser(memberUser);
        notif.setTitle("⚠️ Demande d'inscription en attente");
        notif.setBody(user.getName() + " " + user.getLastName()
            + " souhaite s'inscrire à l'offre \"" + offer.getTitle()
            + "\" mais a un paiement en retard. Consultez la liste des inscrits pour approuver ou refuser.");
        notif.setType(Notification.NotifType.payment);
        notificationRepository.save(notif);
    }

    // ── Count confirmed registrations for an offer ────────
    private long countConfirmed(UUID offerId) {
        return registrationRepository.findByOfferId(offerId)
                .stream()
                .filter(r -> r.getStatus() ==
                    OfferRegistration.RegistrationStatus.confirmed)
                .count();
    }

    // ── Get my registrations ──────────────────────────────
    public List<OfferRegistrationResponse> getMyRegistrations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return registrationRepository.findByUserId(user.getId())
                .stream()
                .map(OfferRegistrationResponse::from)
                .toList();
    }

    // ── Get registrations for a specific offer ────────────
    // Member → only offers in their category
    // Admin  → any offer
    public List<OfferRegistrationResponse> getOfferRegistrations(
            UUID offerId, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));

        boolean isAdmin = user.getRole() == User.UserRole.admin
                       || user.getRole() == User.UserRole.super_admin;

        if (!isAdmin) {
            // Member can see registrations if any of their categories matches
            boolean hasAccess = memberRepository.findByUserId(user.getId())
                    .stream()
                    .anyMatch(m -> m.getCategory().getId()
                                   .equals(offer.getCategory().getId()));
            if (!hasAccess) {
                throw new RuntimeException(
                    "Vous ne pouvez voir que les inscriptions de vos offres");
            }
        }

        return registrationRepository.findByOfferId(offerId)
                .stream()
                .map(OfferRegistrationResponse::from)
                .toList();
    }
}
