package com.saas.billing.config;

import com.saas.billing.SubscriptionBillingService;
import com.saas.billing.adapter.email.SmtpNotificationService;
import com.saas.billing.adapter.jdbc.JdbcCustomerRepository;
import com.saas.billing.adapter.jdbc.JdbcInvoiceRepository;
import com.saas.billing.adapter.payment.TokenPrefixPaymentGateway;
import java.time.Clock;
import java.util.Map;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * Composition root: monta o serviço com os adaptadores reais de produção.
 * Credenciais e endereços vêm de variáveis de ambiente, não do código-fonte.
 */
public final class BillingServiceFactory {

    private BillingServiceFactory() {
    }

    public static SubscriptionBillingService fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static SubscriptionBillingService fromEnvironment(Map<String, String> env) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(required(env, "BILLING_DB_URL"));
        dataSource.setUser(required(env, "BILLING_DB_USER"));
        dataSource.setPassword(required(env, "BILLING_DB_PASSWORD"));

        return new SubscriptionBillingService(
                new JdbcCustomerRepository(dataSource),
                new JdbcInvoiceRepository(dataSource),
                new TokenPrefixPaymentGateway(),
                new SmtpNotificationService(
                        env.getOrDefault("BILLING_SMTP_HOST", "smtp.empresa.com"),
                        Integer.parseInt(env.getOrDefault("BILLING_SMTP_PORT", "587")),
                        env.getOrDefault("BILLING_SMTP_FROM", "billing@empresa.com")),
                Clock.systemDefaultZone());
    }

    private static String required(Map<String, String> env, String name) {
        String value = env.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória não definida: " + name);
        }
        return value;
    }
}
