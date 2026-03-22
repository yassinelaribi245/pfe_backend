package tn.star.star_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import tn.star.star_api.dto.OfferRequest;
import tn.star.star_api.dto.OfferResponse;
import tn.star.star_api.entity.*;
import tn.star.star_api.repository.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository                offerRepository;
    private final OfferCategoryRepository        categoryRepository;
    private final AssociationMemberRepository    memberRepository;
    private final UserRepository                 userRepository;
    private final OfferRegistrationRepository    registrationRepository;
    private final ActivityLogService             logService;

    @Value("${app.upload.dir:uploads/offers}")
    private String uploadDir;

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
        AssociationMember member = memberRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException(
                    "Seuls les membres association ont des offres"));
        return offerRepository
                .findByCreatedById(member.getId(),
                    Sort.by(Sort.Direction.DESC, "createdAt"))
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

        // Save offer first to get the ID
        Offer saved = offerRepository.save(offer);
        String offerId = saved.getId().toString();

        // Move images from temp folder to offer's own folder
        if (req.getCoverImage() != null) {
            String movedCover = moveImageToOfferFolder(
                req.getCoverImage(), offerId);
            saved.setCoverImage(movedCover);
        }
        if (req.getImages() != null && !req.getImages().isEmpty()) {
            try {
                List<String> movedImages = req.getImages().stream()
                    .map(url -> moveImageToOfferFolder(url, offerId))
                    .collect(java.util.stream.Collectors.toList());
                saved.setImages(new com.fasterxml.jackson.databind
                    .ObjectMapper().writeValueAsString(movedImages));
            } catch (Exception ignored) {}
        }

        OfferResponse result = OfferResponse.from(offerRepository.save(saved));
        // Log the creation
        logService.log(ActivityLogService.OFFER_CREATED, user,
            "Offre \"" + saved.getTitle() + "\" — catégorie: "
            + saved.getCategory().getName());
        return result;
    }

    // Move an image from temp/xxx.jpg → {offerId}/xxx.jpg
    private String moveImageToOfferFolder(String url, String offerId) {
        try {
            // url is like /api/images/temp/filename.ext
            String[] parts = url.split("/");
            String filename = parts[parts.length - 1];
            java.nio.file.Path src  = java.nio.file.Paths.get(
                "uploads/offers", "temp", filename).toAbsolutePath();
            java.nio.file.Path dest = java.nio.file.Paths.get(
                "uploads/offers", offerId).toAbsolutePath();
            java.nio.file.Files.createDirectories(dest);
            java.nio.file.Files.move(src, dest.resolve(filename),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "/api/images/" + offerId + "/" + filename;
        } catch (Exception e) {
            return url; // keep original if move fails
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

        String offerId = offer.getId().toString();
        if (req.getCoverImage() != null) {
            offer.setCoverImage(
                moveImageToOfferFolder(req.getCoverImage(), offerId));
        }
        if (req.getImages() != null) {
            try {
                List<String> movedImages = req.getImages().stream()
                    .map(url -> moveImageToOfferFolder(url, offerId))
                    .collect(java.util.stream.Collectors.toList());
                offer.setImages(new com.fasterxml.jackson.databind
                    .ObjectMapper().writeValueAsString(movedImages));
            } catch (Exception ignored) {}
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
        try {
            offer.setStatus(Offer.OfferStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide : " + status);
        }
        OfferResponse result = OfferResponse.from(offerRepository.save(offer));
        User user = userRepository.findByEmail(email).orElse(null);
        logService.log(ActivityLogService.OFFER_STATUS, user,
            "\"" + offer.getTitle() + "\" : "
            + oldStatus + " → " + status);
        return result;
    }

    // ── Delete offer + registrations + images ─────────────
    public void deleteOffer(UUID id, String email) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));
        String title    = offer.getTitle();
        String category = offer.getCategory() != null
            ? offer.getCategory().getName() : "";
        // Delete registrations first
        registrationRepository.deleteByOfferId(id);
        // Delete offer
        offerRepository.deleteById(id);
        // Delete image folder
        deleteOfferImageFolder(id.toString());
        // Log with real user
        User user = userRepository.findByEmail(email).orElse(null);
        logService.log(ActivityLogService.OFFER_DELETED, user,
            "\"" + title + "\" — catégorie: " + category);
    }

    private void deleteOfferImageFolder(String offerId) {
        try {
            Path offerImgPath = Paths.get(uploadDir, offerId)
                .toAbsolutePath().normalize();
            if (Files.exists(offerImgPath)) {
                Files.walk(offerImgPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); }
                        catch (IOException ignored) {}
                    });
            }
        } catch (Exception ignored) {}
    }
}
