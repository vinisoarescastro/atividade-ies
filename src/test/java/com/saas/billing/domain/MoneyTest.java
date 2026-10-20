package com.saas.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void evitaErroDePontoFlutuanteQueOcorriaComDouble() {
        // Com double: 0.1 + 0.2 = 0.30000000000000004
        assertEquals(Money.of("0.30"), Money.of("0.10").plus(Money.of("0.20")));
    }

    @Test
    void operacoesNaoAlteramOValorOriginal() {
        Money original = Money.of("100.00");

        Money result = original.minus(Money.of("10.00"));

        assertEquals(Money.of("100.00"), original);
        assertEquals(Money.of("90.00"), result);
        assertNotSame(original, result);
    }

    @Test
    void igualdadeIgnoraEscala() {
        assertEquals(Money.of("10.0"), Money.of("10.00"));
        assertEquals(Money.of("10.0").hashCode(), Money.of("10.00").hashCode());
    }

    @Test
    void arredondaParaCentavosComArredondamentoBancario() {
        assertEquals(new BigDecimal("10.12"), Money.of("10.125").rounded());
        assertEquals(new BigDecimal("10.14"), Money.of("10.135").rounded());
        assertEquals("10.13", Money.of("10.1251").toString());
    }

    @Test
    void valorNegativoViraZero() {
        assertTrue(Money.of("-5.00").isNegative());
        assertEquals(Money.ZERO, Money.of("-5.00").atLeastZero());
        assertEquals(Money.of("5.00"), Money.of("5.00").atLeastZero());
    }

    @Test
    void rejeitaValorNulo() {
        assertThrows(NullPointerException.class, () -> new Money(null));
    }
}
