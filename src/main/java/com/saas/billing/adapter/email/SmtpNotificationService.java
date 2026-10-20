package com.saas.billing.adapter.email;

import com.saas.billing.domain.Money;
import com.saas.billing.port.NotificationException;
import com.saas.billing.port.NotificationService;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

public class SmtpNotificationService implements NotificationService {

    private static final String SUBJECT = "Sua Fatura Mensal";

    private final String host;
    private final int port;
    private final String from;

    public SmtpNotificationService(String host, int port, String from) {
        this.host = host;
        this.port = port;
        this.from = from;
    }

    @Override
    public void sendInvoiceReceipt(String email, Money amount) {
        try {
            SimpleEmail message = new SimpleEmail();
            message.setHostName(host);
            message.setSmtpPort(port);
            message.setFrom(from);
            message.setSubject(SUBJECT);
            message.setMsg("Olá! Sua fatura foi processada no valor de R$ " + amount);
            message.addTo(email);
            message.send();
        } catch (EmailException e) {
            throw new NotificationException("Erro ao enviar e-mail para " + email, e);
        }
    }
}
