package tn.star.star_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.star.star_api.service.ActivityLogService;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final ActivityLogService logService;

    // GET /api/logs — all logs, newest first (super_admin only)
    @GetMapping
    public ResponseEntity<?> getAllLogs() {
        return ResponseEntity.ok(logService.getAllLogs());
    }
}
