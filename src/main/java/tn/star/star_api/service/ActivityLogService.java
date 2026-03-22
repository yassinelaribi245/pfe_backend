package tn.star.star_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.star.star_api.entity.ActivityLog;
import tn.star.star_api.entity.User;
import tn.star.star_api.repository.ActivityLogRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository logRepository;

    // ── Action constants ──────────────────────────────────
    public static final String LOGIN_SUCCESS    = "Connexion réussie";
    public static final String LOGIN_FAILED     = "Tentative de connexion échouée";
    public static final String OFFER_CREATED    = "Offre créée";
    public static final String OFFER_UPDATED    = "Offre modifiée";
    public static final String OFFER_DELETED    = "Offre supprimée";
    public static final String OFFER_STATUS     = "Statut offre modifié";
    public static final String OFFER_REGISTERED = "Inscription à une offre";
    public static final String OFFER_CANCELLED  = "Inscription annulée";
    public static final String OFFER_WAITLIST   = "Mise en liste d'attente";
    public static final String USER_CREATED     = "Utilisateur créé";
    public static final String USER_DELETED     = "Utilisateur supprimé";
    public static final String ROLE_CHANGED     = "Rôle modifié";
    public static final String PASSWORD_CHANGED = "Mot de passe modifié";

    // ── Save a log entry ──────────────────────────────────
    public void log(String action, User user, String details) {
        try {
            ActivityLog entry = new ActivityLog();
            entry.setAction(action);
            entry.setPerformedBy(user);
            entry.setDetails(details);
            entry.setTargetType("offer");
            logRepository.save(entry);
        } catch (Exception e) {
            System.err.println("Log error: " + e.getMessage());
        }
    }

    // ── Get all logs newest first ─────────────────────────
    public List<Map<String, Object>> getAllLogs() {
        return logRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(l -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id",        l.getId());
                    map.put("action",    l.getAction());

                    User u = l.getPerformedBy();
                    map.put("user",      u != null
                        ? u.getName() + " " + u.getLastName()
                        : "Système");
                    map.put("userEmail", u != null ? u.getEmail() : "");
                    map.put("userRole",  u != null
                        ? u.getRole().name() : "");
                    map.put("detail",    l.getDetails());
                    map.put("createdAt", l.getCreatedAt());
                    return map;
                })
                .toList();
    }
}
