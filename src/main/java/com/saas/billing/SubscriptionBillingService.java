package com.saas.billing;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.Invoice;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.domain.PricingCalculator;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InvoiceRepository;
import com.saas.billing.port.NotificationException;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Caso de uso de cobrança mensal: orquestra precificação, pagamento,
 * registro da fatura e notificação. Não contém regra de cálculo nem I/O direto.
 */
public class SubscriptionBillingService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final Clock clock;
    private final PricingCalculator pricingCalculator = new PricingCalculator();

    public SubscriptionBillingService(CustomerRepository customerRepository,
                                      InvoiceRepository invoiceRepository,
                                      PaymentGateway paymentGateway,
                                      NotificationService notificationService,
                                      Clock clock) {
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentGateway = paymentGateway;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    public List<String> processMonthlyBilling(BillingCommand command) {
        Money amount = pricingCalculator.calculate(command, LocalDate.now(clock));

        Optional<Customer> customer = customerRepository.findById(command.customerId());
        if (customer.isEmpty()) {
            return new ArrayList<>();
        }
        if (!customer.get().isActive()) {
            return new ArrayList<>(List.of("Cliente inativo. Cobrança cancelada: " + command.customerId()));
        }
        return charge(customer.get(), command.paymentCardToken(), amount);
    }

    private List<String> charge(Customer customer, String cardToken, Money amount) {
        List<String> logs = new ArrayList<>();
        boolean approved = paymentGateway.charge(cardToken, amount);
        logs.add(approved
                ? "Pagamento processado com sucesso: R$ " + amount
                : "Falha no pagamento do cartão para: " + customer.id());

        invoiceRepository.save(new Invoice(customer.id(), amount, InvoiceStatus.fromPayment(approved)));
        logs.add(notifyCustomer(customer, amount));
        return logs;
    }

    private String notifyCustomer(Customer customer, Money amount) {
        try {
            notificationService.sendInvoiceReceipt(customer.email(), amount);
            return "E-mail disparado para: " + customer.email();
        } catch (NotificationException e) {
            // A fatura já foi gravada: falha no e-mail não pode desfazer nem repetir a cobrança.
            return "Falha ao enviar e-mail para: " + customer.email() + " (" + e.getMessage() + ")";
        }
    }
}
