package tn.star.star_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.star.star_api.dto.OfferRequest;
import tn.star.star_api.dto.OfferResponse;
import tn.star.star_api.entity.*;
import tn.star.star_api.repository.*;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository             offerRepository;
    private final OfferCategoryRepository     categoryRepository;
    private final AssociationMemberRepository memberRepository;
    private final UserRepository              userRepository;

    // ── Get all offers ────────────────────────────────────
    public List<OfferResponse> getAllOffers() {
        return offerRepository.findAll()
                .stream()
                .map(OfferResponse::from)
                .toList();
    }

    // ── Get offers by status ──────────────────────────────
    public List<OfferResponse> getOffersByStatus(String status) {
        try {
            Offer.OfferStatus s = Offer.OfferStatus.valueOf(status);
            return offerRepository.findByStatus(s)
                    .stream().map(OfferResponse::from).toList();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide : " + status
                + ". Valeurs acceptées : active, suggested, cancelled, finished");
        }
    }

    // ── Get one offer ─────────────────────────────────────
    public OfferResponse getOfferById(UUID id) {
        return OfferResponse.from(offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre introuvable")));
    }

    // ── Get my offers (association member) ────────────────
    public List<OfferResponse> getMyOffers(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        AssociationMember member = memberRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException(
                    "Seuls les membres association ont des offres"));
        // Returns ALL offers for this member's category
        // including those created by admin on their behalf
        // The response will clearly show createdByAdmin when applicable
        return offerRepository.findByCreatedById(member.getId())
                .stream().map(OfferResponse::from).toList();
    }

    // ── Create offer ──────────────────────────────────────
    // - Association member → category auto-assigned from their profile
    // - Admin/SuperAdmin  → must provide categoryId, offer is registered
    //                       under the member responsible for that category,
    //                       but admin's id is stored in created_by_admin
    public OfferResponse createOffer(String email, OfferRequest req) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Validate dates
        if (req.getEndDate() != null
                && req.getEndDate().isBefore(req.getStartDate())) {
            throw new RuntimeException(
                "La date de fin doit être après la date de début");
        }

        Offer offer = new Offer();
        offer.setTitle(req.getTitle());
        offer.setDescription(req.getDescription());
        offer.setStartDate(req.getStartDate());
        offer.setEndDate(req.getEndDate());
        offer.setMaxParticipants(req.getMaxParticipants());
        offer.setPrice(req.getPrice());
        offer.setDocumentNeeded(req.getDocumentNeeded());
        offer.setPaymentMethod(req.getPaymentMethod());
        offer.setStatus(Offer.OfferStatus.active);

        boolean isAdmin = user.getRole() == User.UserRole.admin
                       || user.getRole() == User.UserRole.super_admin;

        if (isAdmin) {
            // Admin must provide a categoryId
            if (req.getCategoryId() == null) {
                throw new RuntimeException(
                    "L'admin doit fournir une catégorie (categoryId)");
            }

            // Find the member responsible for this category
            AssociationMember member = memberRepository
                    .findByCategoryId(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException(
                        "Aucun membre association n'est responsable de cette catégorie"));

            offer.setCategory(member.getCategory());
            offer.setCreatedBy(member);        // registered under the member
            offer.setCreatedByAdmin(user);     // admin id stored for security

        } else {
            // Association member — category auto from their profile
            AssociationMember member = memberRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException(
                        "Seuls les membres association peuvent créer des offres"));

            offer.setCategory(member.getCategory());
            offer.setCreatedBy(member);
            offer.setCreatedByAdmin(null);
        }

        return OfferResponse.from(offerRepository.save(offer));
    }

    // ── Update offer ──────────────────────────────────────
    public OfferResponse updateOffer(UUID id, String email, OfferRequest req) {

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        boolean isAdmin = user.getRole() == User.UserRole.admin
                       || user.getRole() == User.UserRole.super_admin;

        // Member can edit any offer in their category
        // (whether they created it or an admin created it on their behalf)
        boolean isMemberOfCategory = false;
        if (user.getRole() == User.UserRole.association_member) {
            isMemberOfCategory = memberRepository.findByUserId(user.getId())
                .map(m -> m.getCategory().getId()
                            .equals(offer.getCategory().getId()))
                .orElse(false);
        }

        if (!isAdmin && !isMemberOfCategory) {
            throw new RuntimeException(
                "Vous n'êtes pas autorisé à modifier cette offre");
        }

        if (req.getEndDate() != null
                && req.getEndDate().isBefore(req.getStartDate())) {
            throw new RuntimeException(
                "La date de fin doit être après la date de début");
        }

        offer.setTitle(req.getTitle());
        offer.setDescription(req.getDescription());
        offer.setStartDate(req.getStartDate());
        offer.setEndDate(req.getEndDate());
        offer.setMaxParticipants(req.getMaxParticipants());
        offer.setPrice(req.getPrice());
        offer.setDocumentNeeded(req.getDocumentNeeded());
        offer.setPaymentMethod(req.getPaymentMethod());

        return OfferResponse.from(offerRepository.save(offer));
    }

    // ── Change status (admin only) ────────────────────────
    public OfferResponse changeStatus(UUID id, String status) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));
        try {
            offer.setStatus(Offer.OfferStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide : " + status);
        }
        return OfferResponse.from(offerRepository.save(offer));
    }

    // ── Delete offer (admin only) ─────────────────────────
    public void deleteOffer(UUID id) {
        if (!offerRepository.existsById(id)) {
            throw new RuntimeException("Offre introuvable");
        }
        offerRepository.deleteById(id);
    }
}
