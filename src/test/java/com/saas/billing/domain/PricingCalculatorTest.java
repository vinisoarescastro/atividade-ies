package com.saas.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @ParameterizedTest(name = "[{index}] {7}")
    @CsvSource(delimiter = '|', textBlock = """
            # base  | adesão     | cobrança   | atraso | cupom       | ONG   | esperado | cenário
              100.00 | 2026-10-01 | 2026-10-15 | 0      |             | false | 100.00   | sem desconto
              100.00 | 2025-10-01 | 2026-10-15 | 0      |             | false | 100.00   | exatamente 12 meses: sem desconto
              100.00 | 2025-09-01 | 2026-10-15 | 0      |             | false |  90.00   | 13 meses: 10% de fidelidade
              100.00 | 2024-10-01 | 2026-10-15 | 0      |             | false |  90.00   | exatamente 24 meses: ainda 10%
              100.00 | 2024-09-01 | 2026-10-15 | 0      |             | false |  85.00   | 25 meses: 15% de fidelidade
              100.00 | 2026-10-01 | 2026-10-15 | 0      |             | true  |  70.00   | ONG: 30% de desconto
              100.00 | 2024-09-01 | 2026-10-15 | 0      |             | true  |  59.50   | ONG sobre preço com fidelidade
              100.00 | 2026-09-01 | 2026-10-15 | 0      | WELCOME10   | false |  90.00   | WELCOME10 com 1 mês
              100.00 | 2026-09-01 | 2026-10-15 | 0      | welcome10   | false |  90.00   | cupom ignora maiúsculas
              100.00 | 2026-08-01 | 2026-10-15 | 0      | WELCOME10   | false | 100.00   | WELCOME10 expirado (2 meses)
              100.00 | 2026-10-01 | 2026-10-15 | 0      | BLACKFRIDAY | false | 100.00   | BLACKFRIDAY fora de novembro
              100.00 | 2026-11-01 | 2026-11-20 | 0      | BLACKFRIDAY | false |  70.00   | BLACKFRIDAY em novembro: -R$ 30
               20.00 | 2026-11-01 | 2026-11-20 | 0      | BLACKFRIDAY | false |   0.00   | desconto maior que o preço: piso zero
              100.00 | 2026-10-01 | 2026-10-15 | 0      | XPTO        | false | 100.00   | cupom desconhecido
              100.00 | 2026-10-01 | 2026-10-15 | 0      | '   '       | false | 100.00   | cupom em branco
              100.00 | 2026-10-01 | 2026-10-15 | 10     |             | false | 103.00   | atraso: 2% multa + 0,1%/dia
              100.00 | 2026-10-01 | 2026-10-15 | -5     |             | false | 100.00   | atraso negativo é ignorado
               20.00 | 2026-11-01 | 2026-11-20 | 10     | BLACKFRIDAY | false |   0.00   | juros sobre valor negativo: piso zero
               49.90 | 2024-09-01 | 2026-10-15 | 7      |             | true  |  30.49   | combinação completa com arredondamento
            """)
    void calculaPreco(String base, LocalDate signup, LocalDate billingDate, int overdueDays,
                      String promoCode, boolean nonProfit, String expected, String scenario) {
        BillingCommand command = new BillingCommand(
                "c1", Money.of(base), signup, overdueDays, promoCode, nonProfit, "tok_x");

        Money price = calculator.calculate(command, billingDate);

        assertEquals(expected, price.toString(), scenario);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-09-30, 2026-10-01, 1",
            "2026-10-31, 2026-10-01, 0",
            "2024-12-15, 2026-01-15, 13"})
    void mesesAtivosIgnoramODiaDoMes(LocalDate signup, LocalDate billingDate, int expected) {
        assertEquals(expected, PricingCalculator.monthsActive(signup, billingDate));
    }
}
