package tn.star.star_api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Value("${app.upload.dir:uploads/offers}")
    private String uploadDir;

    // ── POST multipart (normal) ───────────────────────────
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "offerId",
                defaultValue = "temp") String offerId) {
        return saveFiles(files, offerId);
    }

    // ── POST base64 JSON (Cloudflare-safe fallback) ───────
    // Body: { "offerId": "temp", "files": [ { "name": "x.jpg", "data": "base64..." } ] }
    @PostMapping("/upload-base64")
    public ResponseEntity<?> uploadBase64(
            @RequestBody Map<String, Object> body) {
        try {
            String offerId = (String) body.getOrDefault("offerId", "temp");
            Path offerPath = Paths.get(uploadDir, offerId)
                    .toAbsolutePath().normalize();
            Files.createDirectories(offerPath);

            List<?> files = (List<?>) body.get("files");
            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("message", "Aucun fichier fourni"));
            }

            List<String> urls = new ArrayList<>();
            for (Object f : files) {
                @SuppressWarnings("unchecked")
                Map<String, String> file = (Map<String, String>) f;
                String name   = file.getOrDefault("name", "image.jpg");
                String data   = file.get("data"); // base64 string
                if (data == null) continue;

                // Strip data URI prefix if present
                if (data.contains(",")) data = data.split(",")[1];

                byte[] bytes = Base64.getDecoder().decode(data);
                if (bytes.length > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body(
                        Map.of("message", "Max 5MB par image"));
                }

                String ext      = getExtension(name);
                String filename = UUID.randomUUID() + "." + ext;
                Files.write(offerPath.resolve(filename), bytes);
                urls.add("/api/images/" + offerId + "/" + filename);
            }

            return ResponseEntity.ok(Map.of("urls", urls));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                Map.of("message", "Erreur: " + e.getMessage()));
        }
    }

    // ── GET /api/images/{offerId}/{filename} ──────────────
    @GetMapping("/{offerId}/{filename:.+}")
    public ResponseEntity<Resource> serveImage(
            @PathVariable String offerId,
            @PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir, offerId, filename)
                    .toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "image/jpeg";

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── DELETE folder for an offer ────────────────────────
    @DeleteMapping("/{offerId}")
    public ResponseEntity<?> deleteOfferImages(
            @PathVariable String offerId) {
        try {
            Path offerPath = Paths.get(uploadDir, offerId)
                    .toAbsolutePath().normalize();
            deleteDirectory(offerPath);
            return ResponseEntity.ok(Map.of("message", "Supprimé"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                Map.of("message", "Erreur: " + e.getMessage()));
        }
    }

    private ResponseEntity<?> saveFiles(
            List<MultipartFile> files, String offerId) {
        try {
            Path offerPath = Paths.get(uploadDir, offerId)
                    .toAbsolutePath().normalize();
            Files.createDirectories(offerPath);
            List<String> urls = new ArrayList<>();
            for (MultipartFile file : files) {
                String ct = file.getContentType();
                if (ct == null || !ct.startsWith("image/"))
                    return ResponseEntity.badRequest().body(
                        Map.of("message", "Images uniquement"));
                if (file.getSize() > 5 * 1024 * 1024)
                    return ResponseEntity.badRequest().body(
                        Map.of("message", "Max 5MB"));
                String ext  = getExtension(
                    file.getOriginalFilename());
                String name = UUID.randomUUID() + "." + ext;
                Files.copy(file.getInputStream(),
                    offerPath.resolve(name),
                    StandardCopyOption.REPLACE_EXISTING);
                urls.add("/api/images/" + offerId + "/" + name);
            }
            return ResponseEntity.ok(Map.of("urls", urls));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                Map.of("message", "Erreur: " + e.getMessage()));
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walk(path).sorted(Comparator.reverseOrder())
            .forEach(p -> { try { Files.delete(p); }
                catch (IOException ignored) {} });
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(
            filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
