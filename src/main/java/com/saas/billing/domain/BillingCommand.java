package com.saas.billing.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Parameter Object com todos os dados de entrada de uma cobrança mensal.
 * Substitui os 8 parâmetros soltos do método legado.
 */
public record BillingCommand(
        String customerId,
        Money basePrice,
        LocalDate signupDate,
        int overdueDays,
        String promoCode,
        boolean nonProfit,
        String paymentCardToken) {

    public BillingCommand {
        Objects.requireNonNull(customerId, "customerId não pode ser nulo");
        Objects.requireNonNull(basePrice, "basePrice não pode ser nulo");
        Objects.requireNonNull(signupDate, "signupDate não pode ser nulo");
    }

    public boolean hasPromoCode() {
        return promoCode != null && !promoCode.isBlank();
    }

    public boolean isOverdue() {
        return overdueDays > 0;
    }
}
