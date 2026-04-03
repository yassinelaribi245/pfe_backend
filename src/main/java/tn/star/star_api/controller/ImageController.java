package tn.star.star_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tn.star.star_api.entity.Offer;
import tn.star.star_api.repository.OfferRepository;

import java.util.*;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final OfferRepository offerRepository;
    private final ObjectMapper    mapper = new ObjectMapper();

    // ── POST /api/images/upload-base64 ────────────────────────────────────
    // Body : { "offerId": "uuid-or-temp",
    //          "files": [{"name":"x.jpg", "data":"base64..."}] }
    //
    // Validates files and returns both placeholder URLs AND the raw base64
    // so Flutter can embed them directly in the offer create/update body.
    @PostMapping("/upload-base64")
    public ResponseEntity<?> uploadBase64(
            @RequestBody Map<String, Object> body) {
        try {
            String offerId = (String) body.getOrDefault("offerId", "temp");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> files =
                (List<Map<String, String>>) body.get("files");

            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("message", "Aucun fichier fourni"));
            }

            // Validate each file size (max 5 MB)
            for (Map<String, String> f : files) {
                String data = f.get("data");
                if (data == null) continue;
                if (data.contains(",")) data = data.split(",")[1];
                byte[] decoded = Base64.getDecoder().decode(data);
                if (decoded.length > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body(
                        Map.of("message", "Max 5MB par image — "
                            + f.getOrDefault("name", "fichier")
                            + " depasse la limite"));
                }
            }

            // Return placeholder URLs + raw base64 list for Flutter to embed
            // directly into OfferRequest.coverImage / OfferRequest.images
            List<String> urls = new ArrayList<>();
            List<String> b64s = new ArrayList<>();

            for (int i = 0; i < files.size(); i++) {
                b64s.add(files.get(i).getOrDefault("data", ""));
                if (i == 0) {
                    urls.add("/api/images/" + offerId + "/cover");
                } else {
                    urls.add("/api/images/" + offerId + "/gallery/" + (i - 1));
                }
            }

            return ResponseEntity.ok(Map.of(
                "urls",   urls,
                "base64", b64s,  // Flutter must pass these into coverImage/images
                "files",  files  // kept for backward compatibility
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("message", "Erreur upload: " + e.getMessage()));
        }
    }

    // ── GET /api/images/{offerId}/cover ───────────────────────────────────
    @Transactional(readOnly = true)
    @GetMapping("/{offerId}/cover")
    public ResponseEntity<byte[]> getCover(
            @PathVariable String offerId) {

        UUID id = parseUuid(offerId);
        if (id == null) return ResponseEntity.notFound().build();

        Offer offer = offerRepository.findById(id).orElse(null);
        if (offer == null) return ResponseEntity.notFound().build();

        byte[] data = offer.getCoverImage();
        if (data == null || data.length == 0)
            return ResponseEntity.notFound().build();

        String ct = offer.getCoverImageType() != null
            ? offer.getCoverImageType() : "image/jpeg";

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(ct))
            .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)))
            .body(data);
    }

    // ── GET /api/images/{offerId}/gallery/{index} ─────────────────────────
    @Transactional(readOnly = true)
    @GetMapping("/{offerId}/gallery/{index}")
    public ResponseEntity<byte[]> getGalleryImage(
            @PathVariable String offerId,
            @PathVariable int index) {

        UUID id = parseUuid(offerId);
        if (id == null) return ResponseEntity.notFound().build();

        Offer offer = offerRepository.findById(id).orElse(null);
        if (offer == null || offer.getImages() == null)
            return ResponseEntity.notFound().build();

        try {
            @SuppressWarnings("unchecked")
            List<String> images = mapper.readValue(offer.getImages(), List.class);
            @SuppressWarnings("unchecked")
            List<String> types = offer.getImagesTypes() != null
                ? mapper.readValue(offer.getImagesTypes(), List.class)
                : Collections.emptyList();

            if (index < 0 || index >= images.size())
                return ResponseEntity.notFound().build();

            String b64 = images.get(index);
            if (b64.contains(",")) b64 = b64.split(",")[1];
            byte[] bytes = Base64.getDecoder().decode(b64);

            String ct = index < types.size()
                ? types.get(index) : "image/jpeg";

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)))
                .body(bytes);

        } catch (Exception e) {
            System.err.println("Gallery image error [" + offerId
                + "/" + index + "]: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────
    private UUID parseUuid(String s) {
        try { return UUID.fromString(s); }
        catch (Exception e) { return null; }
    }
}
