package com.saas.billing.port;

import com.saas.billing.domain.Money;

/** Porta de saída para envio do recibo da fatura ao cliente. */
public interface NotificationService {

    /** @throws NotificationException se o envio falhar */
    void sendInvoiceReceipt(String email, Money amount);
}
