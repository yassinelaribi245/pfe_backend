package tn.star.star_api.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.star.star_api.entity.Offer;
import tn.star.star_api.repository.OfferRepository;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OfferScheduler {

    private static final Logger log = LoggerFactory.getLogger(OfferScheduler.class);

    private final OfferRepository offerRepository;
    private final OfferService    offerService;

    // ── Run immediately on startup — catches any offers that expired
    //    while the server was offline ────────────────────────────────────────
    @PostConstruct
    public void runOnStartup() {
        log.info("OfferScheduler: checking expired offers on startup...");
        closeExpiredOffers();
    }

    // ── Run every hour ──────────────────────────────────────────────────────
    // cron = "0 0 * * * *"  →  top of every hour
    // For local testing you can change to "0 * * * * *" (every minute)
    @Scheduled(cron = "0 0 * * * *")
    public void scheduledCheck() {
        log.info("OfferScheduler: hourly check for expired offers...");
        closeExpiredOffers();
    }

    // ── Core logic ──────────────────────────────────────────────────────────
    private void closeExpiredOffers() {
        LocalDate today = LocalDate.now();
        List<Offer> expired = offerRepository.findExpiredActiveOffers(today);

        if (expired.isEmpty()) {
            log.info("OfferScheduler: no expired offers found.");
            return;
        }

        log.info("OfferScheduler: closing {} expired offer(s)...", expired.size());
        for (Offer offer : expired) {
            try {
                offerService.closeExpiredOffer(offer);
                log.info("OfferScheduler: closed offer \"{}\" (end_date: {})",
                    offer.getTitle(), offer.getEndDate());
            } catch (Exception e) {
                log.error("OfferScheduler: failed to close offer \"{}\" — {}",
                    offer.getTitle(), e.getMessage());
            }
        }
    }
}
