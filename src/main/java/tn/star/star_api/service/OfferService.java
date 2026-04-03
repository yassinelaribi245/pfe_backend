package tn.star.star_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import tn.star.star_api.dto.OfferRequest;
import tn.star.star_api.dto.OfferResponse;
import tn.star.star_api.entity.*;
import tn.star.star_api.repository.*;
import org.springframework.context.annotation.Lazy;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository                offerRepository;
    private final OfferCategoryRepository        categoryRepository;
    private final AssociationMemberRepository    memberRepository;
    private final UserRepository                 userRepository;
    private final OfferRegistrationRepository    registrationRepository;
    private final ActivityLogService             logService;
    @Lazy
    private final PaymentService                 paymentService;

    // ── Get all offers — newest first ─────────────────────
    public List<OfferResponse> getAllOffers() {
        return offerRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(OfferResponse::from)
                .toList();
    }

    // ── Get offers by status — newest first ───────────────
    public List<OfferResponse> getOffersByStatus(String status) {
        try {
            Offer.OfferStatus s = Offer.OfferStatus.valueOf(status);
            return offerRepository
                    .findByStatus(s, Sort.by(Sort.Direction.DESC, "createdAt"))
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

    // ── Get my offers — newest first ──────────────────────
    public List<OfferResponse> getMyOffers(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        // A user can have multiple memberships — collect all offer IDs
        List<AssociationMember> memberships = memberRepository.findByUserId(user.getId());
        if (memberships.isEmpty()) {
            throw new RuntimeException("Seuls les membres association ont des offres");
        }
        return memberships.stream()
            .flatMap(m -> offerRepository.findByCreatedById(m.getId(),
                Sort.by(Sort.Direction.DESC, "createdAt")).stream())
            .distinct()
            .map(OfferResponse::from)
            .toList();
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
        saveAllowedMethods(offer, req.getAllowedPaymentMethods(), req.getPaymentMethod());
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
            // Pick the first available member for this category
            AssociationMember member = memberRepository
                    .findFirstByCategoryId(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException(
                        "Aucun membre association n'est responsable de cette catégorie"));

            offer.setCategory(member.getCategory());
            offer.setCreatedBy(member);        // registered under the member
            offer.setCreatedByAdmin(user);     // admin id stored for security

        } else {
            // Association member — category auto from their profile
            List<AssociationMember> memberships = memberRepository.findByUserId(user.getId());
            if (memberships.isEmpty()) {
                throw new RuntimeException("Seuls les membres association peuvent créer des offres");
            }
            // Use the first membership (member can have multiple categories)
            AssociationMember member = memberships.get(0);

            offer.setCategory(member.getCategory());
            offer.setCreatedBy(member);
            offer.setCreatedByAdmin(null);
        }

        // Save offer to get ID, then persist images into DB
        Offer saved = offerRepository.save(offer);
        saveImagesToOffer(saved, req.getCoverImage(), req.getImages());

        OfferResponse result = OfferResponse.from(offerRepository.save(saved));
        // Log the creation
        logService.log(ActivityLogService.OFFER_CREATED, user,
            "Offre \"" + saved.getTitle() + "\" — catégorie: "
            + saved.getCategory().getName());
        return result;
    }

    // ── Save images into DB fields ────────────────────────
    private void saveImagesToOffer(Offer offer,
            String coverB64, List<String> galleryB64List) {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            // Cover image
            if (coverB64 != null && !coverB64.isEmpty()) {
                String raw  = coverB64.contains(",")
                    ? coverB64.split(",")[1] : coverB64;
                String type = coverB64.startsWith("data:")
                    ? coverB64.split(";")[0].substring(5)
                    : "image/jpeg";
                offer.setCoverImage(
                    java.util.Base64.getDecoder().decode(raw));
                offer.setCoverImageType(type);
            }
            // Gallery — store each as base64 string in JSON array
            if (galleryB64List != null && !galleryB64List.isEmpty()) {
                List<String> dataList  = new java.util.ArrayList<>();
                List<String> typeList  = new java.util.ArrayList<>();
                for (String b64 : galleryB64List) {
                    String raw  = b64.contains(",")
                        ? b64.split(",")[1] : b64;
                    String type = b64.startsWith("data:")
                        ? b64.split(";")[0].substring(5)
                        : "image/jpeg";
                    dataList.add(raw);
                    typeList.add(type);
                }
                offer.setImages(mapper.writeValueAsString(dataList));
                offer.setImagesTypes(mapper.writeValueAsString(typeList));
            }
        } catch (Exception e) {
            System.err.println("Image save error: " + e.getMessage());
        }
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
                .stream()
                .anyMatch(m -> m.getCategory().getId()
                            .equals(offer.getCategory().getId()));
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
        saveAllowedMethods(offer, req.getAllowedPaymentMethods(), req.getPaymentMethod());

        // Update images if new ones provided
        if (req.getCoverImage() != null || req.getImages() != null) {
            saveImagesToOffer(offer, req.getCoverImage(), req.getImages());
        }

        OfferResponse updated = OfferResponse.from(offerRepository.save(offer));
        logService.log(ActivityLogService.OFFER_UPDATED, user,
            "Offre \"" + offer.getTitle() + "\"");
        return updated;
    }

    // ── Change status ─────────────────────────────────────
    public OfferResponse changeStatus(UUID id, String status, String email) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));
        String oldStatus = offer.getStatus().name();
        Offer.OfferStatus newStatus;
        try {
            newStatus = Offer.OfferStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide : " + status
                + ". Valeurs acceptées : active, suggested, cancelled, finished");
        }
        offer.setStatus(newStatus);
        OfferResponse result = OfferResponse.from(offerRepository.save(offer));

        // When offer is manually closed → generate payment installments
        if (newStatus == Offer.OfferStatus.finished
                && offer.getPaymentMethod() != Offer.PaymentMethod.free) {
            paymentService.generatePaymentsForOffer(id);
        }

        User user = userRepository.findByEmail(email).orElse(null);
        logService.log(ActivityLogService.OFFER_STATUS, user,
            "\"" + offer.getTitle() + "\" : "
            + oldStatus + " → " + status);
        return result;
    }


    // ── Helper: save allowed payment methods ──────────────────────────────
    private void saveAllowedMethods(Offer offer,
            java.util.List<String> methods,
            tn.star.star_api.entity.Offer.PaymentMethod pm) {
        if (methods != null && !methods.isEmpty()) {
            try {
                offer.setAllowedPaymentMethods(
                    new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(methods));
            } catch (Exception ignored) {}
        } else if (pm == tn.star.star_api.entity.Offer.PaymentMethod.free) {
            offer.setAllowedPaymentMethods("[\"free\"]");
        }
    }
    // ── Called by scheduler — closes expired offers and generates payments ──
    public void closeExpiredOffer(Offer offer) {
        offer.setStatus(Offer.OfferStatus.finished);
        offerRepository.save(offer);
        if (offer.getPaymentMethod() != Offer.PaymentMethod.free) {
            paymentService.generatePaymentsForOffer(offer.getId());
        }
        logService.log(ActivityLogService.OFFER_STATUS, null,
            "\"" + offer.getTitle() + "\" : active → finished (expiration automatique)");
    }

    // ── Delete offer + registrations + images ─────────────
    public void deleteOffer(UUID id, String email) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));
        String title    = offer.getTitle();
        String category = offer.getCategory() != null
            ? offer.getCategory().getName() : "";
        // Delete registrations first (FK constraint)
        registrationRepository.deleteByOfferId(id);
        // Delete offer — images stored as BYTEA are deleted automatically
        offerRepository.deleteById(id);
        // Log with real user
        User user = userRepository.findByEmail(email).orElse(null);
        logService.log(ActivityLogService.OFFER_DELETED, user,
            "\"" + title + "\" — catégorie: " + category);
    }

}
