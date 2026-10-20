package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saas.billing.adapter.payment.TokenPrefixPaymentGateway;
import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.Invoice;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.fakes.InMemoryCustomerRepository;
import com.saas.billing.fakes.InMemoryInvoiceRepository;
import com.saas.billing.fakes.RecordingNotificationService;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InfrastructureException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionBillingServiceTest {

    private static final Clock OCTOBER_15 = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);
    private static final Clock NOVEMBER_20 = Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC);

    private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository()
            .with(new Customer("c1", "ana@cliente.com", "ACTIVE"))
            .with(new Customer("c2", "bia@cliente.com", "CANCELLED"));
    private final InMemoryInvoiceRepository invoices = new InMemoryInvoiceRepository();
    private RecordingNotificationService notifications = new RecordingNotificationService();

    private SubscriptionBillingService service(Clock clock) {
        return new SubscriptionBillingService(customers, invoices, new TokenPrefixPaymentGateway(), notifications, clock);
    }

    private static BillingCommand command(String customerId, String promoCode, String cardToken) {
        return new BillingCommand(customerId, Money.of("100.00"), LocalDate.of(2026, 10, 1), 0, promoCode, false, cardToken);
    }

    @Test
    void pagamentoAprovadoGravaFaturaPagaENotificaCliente() {
        List<String> logs = service(OCTOBER_15).processMonthlyBilling(command("c1", null, "tok_visa"));

        assertEquals(List.of(
                "Pagamento processado com sucesso: R$ 100.00",
                "E-mail disparado para: ana@cliente.com"), logs);
        assertEquals(List.of(new Invoice("c1", Money.of("100.00"), InvoiceStatus.PAID)), invoices.saved());
        assertEquals(List.of(new RecordingNotificationService.Sent("ana@cliente.com", Money.of("100.00"))), notifications.sent());
    }

    @Test
    void pagamentoRecusadoGravaFaturaFalhaEAindaNotifica() {
        List<String> logs = service(OCTOBER_15).processMonthlyBilling(command("c1", null, "cartao_invalido"));

        assertEquals(List.of(
                "Falha no pagamento do cartão para: c1",
                "E-mail disparado para: ana@cliente.com"), logs);
        assertEquals(InvoiceStatus.FAILED, invoices.saved().get(0).status());
        assertEquals(1, notifications.sent().size());
    }

    @Test
    void tokenNuloEhPagamentoRecusado() {
        service(OCTOBER_15).processMonthlyBilling(command("c1", null, null));

        assertEquals(InvoiceStatus.FAILED, invoices.saved().get(0).status());
    }

    @Test
    void clienteInativoNaoECobrado() {
        List<String> logs = service(OCTOBER_15).processMonthlyBilling(command("c2", null, "tok_visa"));

        assertEquals(List.of("Cliente inativo. Cobrança cancelada: c2"), logs);
        assertTrue(invoices.saved().isEmpty());
        assertTrue(notifications.sent().isEmpty());
    }

    @Test
    void clienteInexistenteNaoGeraNada() {
        List<String> logs = service(OCTOBER_15).processMonthlyBilling(command("nao-existe", null, "tok_visa"));

        assertTrue(logs.isEmpty());
        assertTrue(invoices.saved().isEmpty());
        assertTrue(notifications.sent().isEmpty());
    }

    @Test
    void falhaNoEmailNaoDesfazACobranca() {
        notifications = new RecordingNotificationService().failing();

        List<String> logs = service(OCTOBER_15).processMonthlyBilling(command("c1", null, "tok_visa"));

        assertEquals(InvoiceStatus.PAID, invoices.saved().get(0).status());
        assertTrue(logs.get(1).startsWith("Falha ao enviar e-mail para: ana@cliente.com"));
    }

    @Test
    void falhaDeBancoNaoESilenciada() {
        CustomerRepository brokenDatabase = id -> {
            throw new InfrastructureException("conexão recusada", null);
        };
        SubscriptionBillingService service = new SubscriptionBillingService(
                brokenDatabase, invoices, new TokenPrefixPaymentGateway(), notifications, OCTOBER_15);

        assertThrows(InfrastructureException.class, () -> service.processMonthlyBilling(command("c1", null, "tok_visa")));
        assertTrue(invoices.saved().isEmpty());
    }

    @Test
    void relogioInjetadoTornaCupomSazonalDeterministico() {
        BillingCommand blackFriday = new BillingCommand(
                "c1", Money.of("100.00"), LocalDate.of(2026, 10, 1), 0, "BLACKFRIDAY", false, "tok_visa");

        service(OCTOBER_15).processMonthlyBilling(blackFriday);
        service(NOVEMBER_20).processMonthlyBilling(blackFriday);

        assertEquals(Money.of("100.00"), invoices.saved().get(0).amount()); // outubro: cupom não vale
        assertEquals(Money.of("70.00"), invoices.saved().get(1).amount());  // novembro: R$ 30 de desconto
    }
}
