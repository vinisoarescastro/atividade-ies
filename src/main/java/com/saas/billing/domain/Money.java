package com.saas.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object imutável que representa um valor monetário em reais.
 *
 * As operações são feitas com {@link BigDecimal} sem arredondamento intermediário,
 * para não acumular erros de ponto flutuante como acontecia com {@code double}.
 * O arredondamento para centavos (HALF_EVEN, o "arredondamento bancário") é aplicado
 * apenas na saída, via {@link #rounded()} e {@link #toString()}.
 */
public record Money(BigDecimal amount) {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount não pode ser nulo");
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money times(String factor) {
        return times(new BigDecimal(factor));
    }

    public Money times(int factor) {
        return times(BigDecimal.valueOf(factor));
    }

    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    /** Valores negativos viram zero: uma cobrança nunca é um crédito ao cliente. */
    public Money atLeastZero() {
        return isNegative() ? ZERO : this;
    }

    /** Valor arredondado para centavos, pronto para persistir ou exibir. */
    public BigDecimal rounded() {
        return amount.setScale(SCALE, ROUNDING);
    }

    /** Dois valores são iguais quando representam a mesma quantia, independente da escala (10.0 == 10.00). */
    @Override
    public boolean equals(Object other) {
        return other instanceof Money money && amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return rounded().toPlainString();
    }
}
