package tn.star.star_api.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import tn.star.star_api.entity.OfferPayment;
import tn.star.star_api.entity.OfferRegistration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class OfferPaymentResponse {

    private UUID   registrationId;
    private UUID   userId;
    private String userName;
    private String userEmail;
    private String registrationStatus;   // confirmed / waitlist / pending_approval
    private OffsetDateTime registeredAt;

    // All installments for this registration
    private List<Installment> installments;

    // Convenience totals
    private int     totalInstallments;
    private int     paidInstallments;
    private boolean fullyPaid;
    private boolean hasOverdue;

    @Data
    @AllArgsConstructor
    public static class Installment {
        private UUID        id;
        private int         number;      // 1-based
        private int         total;
        private BigDecimal  amount;
        private LocalDate   dueDate;
        private OffsetDateTime paidAt;
        private String      markedByName;
        private String      status;      // pending / paid / overdue
    }

    public static OfferPaymentResponse from(
            OfferRegistration reg,
            List<OfferPayment> payments) {

        List<Installment> installments = payments.stream()
            .map(p -> new Installment(
                p.getId(),
                p.getInstallmentNumber(),
                p.getTotalInstallments(),
                p.getAmount(),
                p.getDueDate(),
                p.getPaidAt(),
                p.getMarkedBy() != null
                    ? p.getMarkedBy().getName() + " " + p.getMarkedBy().getLastName()
                    : null,
                p.getStatus().name()
            ))
            .collect(Collectors.toList());

        long paid    = payments.stream()
            .filter(p -> p.getStatus() == OfferPayment.PaymentStatus.paid).count();
        boolean over = payments.stream()
            .anyMatch(p -> p.getStatus() == OfferPayment.PaymentStatus.overdue);

        return new OfferPaymentResponse(
            reg.getId(),
            reg.getUser().getId(),
            reg.getUser().getName() + " " + reg.getUser().getLastName(),
            reg.getUser().getEmail(),
            reg.getStatus().name(),
            reg.getDate(),
            installments,
            installments.size(),
            (int) paid,
            !installments.isEmpty() && paid == installments.size(),
            over
        );
    }
}
