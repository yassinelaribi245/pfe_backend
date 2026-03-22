package tn.star.star_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.star.star_api.entity.Notification;
import tn.star.star_api.entity.User;
import tn.star.star_api.repository.NotificationRepository;
import tn.star.star_api.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;

    // GET /api/notifications — get my notifications
    @GetMapping
    public ResponseEntity<?> getMyNotifications(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        List<Notification> notifs = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(notifs.stream().map(n -> {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id",        n.getId());
            map.put("title",     n.getTitle());
            map.put("body",      n.getBody());
            map.put("type",      n.getType().name());
            map.put("isRead",    n.getIsRead());
            map.put("createdAt", n.getCreatedAt());
            return map;
        }).toList());
    }

    // PATCH /api/notifications/{id}/read — mark as read
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable UUID id,
                                      Authentication auth) {
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification introuvable"));
        notif.setIsRead(true);
        notificationRepository.save(notif);
        return ResponseEntity.ok("Notification marquée comme lue");
    }
}
