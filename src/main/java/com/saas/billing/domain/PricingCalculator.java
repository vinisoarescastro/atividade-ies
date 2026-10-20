package com.saas.billing.domain;

import java.time.LocalDate;
import java.time.Month;

/**
 * Motor de precificação da assinatura mensal. Classe de domínio pura:
 * sem banco, rede ou relógio do sistema — a data de cobrança é recebida
 * como parâmetro, então o mesmo comando sempre gera o mesmo valor.
 *
 * Ordem de aplicação (mantida do código legado):
 * fidelidade → ONG → cupom → multa/juros por atraso → piso em zero.
 */
public class PricingCalculator {

    static final String BLACK_FRIDAY = "BLACKFRIDAY";
    static final String WELCOME_10 = "WELCOME10";
    private static final Money BLACK_FRIDAY_DISCOUNT = Money.of("30.00");

    public Money calculate(BillingCommand command, LocalDate billingDate) {
        int monthsActive = monthsActive(command.signupDate(), billingDate);

        Money price = applyLoyaltyDiscount(command.basePrice(), monthsActive);
        price = applyNonProfitDiscount(price, command.nonProfit());
        if (command.hasPromoCode()) {
            price = applyPromoCode(price, command.promoCode(), billingDate, monthsActive);
        }
        price = applyLateFees(price, command.overdueDays());
        return price.atLeastZero();
    }

    /** Diferença em meses de calendário; o dia do mês é ignorado (regra legada). */
    static int monthsActive(LocalDate signupDate, LocalDate billingDate) {
        return (billingDate.getYear() - signupDate.getYear()) * 12
                + (billingDate.getMonthValue() - signupDate.getMonthValue());
    }

    private Money applyLoyaltyDiscount(Money basePrice, int monthsActive) {
        if (monthsActive > 24) {
            return basePrice.minus(basePrice.times("0.15"));
        }
        if (monthsActive > 12) {
            return basePrice.minus(basePrice.times("0.10"));
        }
        return basePrice;
    }

    private Money applyNonProfitDiscount(Money price, boolean nonProfit) {
        return nonProfit ? price.times("0.70") : price;
    }

    private Money applyPromoCode(Money price, String promoCode, LocalDate billingDate, int monthsActive) {
        if (promoCode.equalsIgnoreCase(BLACK_FRIDAY) && billingDate.getMonth() == Month.NOVEMBER) {
            return price.minus(BLACK_FRIDAY_DISCOUNT);
        }
        if (promoCode.equalsIgnoreCase(WELCOME_10) && monthsActive <= 1) {
            return price.times("0.90");
        }
        return price;
    }

    /** Multa de 2% + juros de 0,1% ao dia, ambos sobre o valor já com descontos. */
    private Money applyLateFees(Money price, int overdueDays) {
        if (overdueDays <= 0) {
            return price;
        }
        Money penalty = price.times("0.02");
        Money interest = price.times("0.001").times(overdueDays);
        return price.plus(penalty).plus(interest);
    }
}
