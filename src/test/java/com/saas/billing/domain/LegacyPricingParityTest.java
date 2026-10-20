package com.saas.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Teste de caracterização: garante que o PricingCalculator reproduz exatamente
 * a regra do código legado em todas as combinações relevantes de entrada.
 */
class LegacyPricingParityTest {

    private static final double[] BASE_PRICES = {0.0, 9.90, 49.90, 100.0, 199.99};
    private static final int[] MONTHS_ACTIVE = {0, 1, 2, 12, 13, 24, 25, 60};
    private static final String[] PROMO_CODES = {null, "", "   ", "BLACKFRIDAY", "blackfriday", "WELCOME10", "XPTO"};
    private static final int[] OVERDUE_DAYS = {-3, 0, 1, 15};
    private static final LocalDate[] BILLING_DATES = {LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 20)};

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void reproduzOCalculoLegadoEmTodasAsCombinacoes() {
        int cases = 0;
        for (double base : BASE_PRICES)
            for (int months : MONTHS_ACTIVE)
                for (boolean nonProfit : new boolean[] {false, true})
                    for (String promo : PROMO_CODES)
                        for (int overdue : OVERDUE_DAYS)
                            for (LocalDate today : BILLING_DATES) {
                                LocalDate signup = today.minusMonths(months);
                                double legacy = legacyPrice(base, signup, today, overdue, promo, nonProfit);

                                Money actual = calculator.calculate(
                                        new BillingCommand("c1", Money.of(base), signup, overdue, promo, nonProfit, "tok_x"),
                                        today);

                                assertEquals(legacy, actual.amount().doubleValue(), 1e-9,
                                        () -> "base=%s meses=%d ong=%s cupom=%s atraso=%d data=%s"
                                                .formatted(base, months, nonProfit, promo, overdue, today));
                                cases++;
                            }
        assertEquals(4480, cases);
    }

    /** Cópia literal do cálculo do SubscriptionBillingService legado (commit 1), só sem I/O. */
    private static double legacyPrice(double basePrice, LocalDate signupDate, LocalDate today,
                                      int overdueDays, String promoCode, boolean isNonProfit) {
        int monthsActive = (today.getYear() - signupDate.getYear()) * 12 + (today.getMonthValue() - signupDate.getMonthValue());

        double calculatedPrice = basePrice;

        if (monthsActive > 24) {
            calculatedPrice = calculatedPrice - (basePrice * 0.15);
        } else if (monthsActive > 12) {
            calculatedPrice = calculatedPrice - (basePrice * 0.10);
        }

        if (isNonProfit) {
            calculatedPrice = calculatedPrice * 0.70;
        }

        if (promoCode != null && !promoCode.trim().isEmpty()) {
            if (promoCode.equalsIgnoreCase("BLACKFRIDAY") && today.getMonthValue() == 11) {
                calculatedPrice = calculatedPrice - 30.0;
            } else if (promoCode.equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
                calculatedPrice = calculatedPrice * 0.90;
            }
        }

        if (overdueDays > 0) {
            double penalty = calculatedPrice * 0.02;
            double dailyInterest = (calculatedPrice * 0.001) * overdueDays;
            calculatedPrice = calculatedPrice + penalty + dailyInterest;
        }

        if (calculatedPrice < 0) {
            calculatedPrice = 0.0;
        }
        return calculatedPrice;
    }
}
