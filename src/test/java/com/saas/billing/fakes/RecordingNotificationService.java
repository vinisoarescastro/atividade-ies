package com.saas.billing.fakes;

import com.saas.billing.domain.Money;
import com.saas.billing.port.NotificationException;
import com.saas.billing.port.NotificationService;
import java.util.ArrayList;
import java.util.List;

public class RecordingNotificationService implements NotificationService {

    public record Sent(String email, Money amount) {
    }

    private final List<Sent> sent = new ArrayList<>();
    private boolean failing;

    public RecordingNotificationService failing() {
        this.failing = true;
        return this;
    }

    @Override
    public void sendInvoiceReceipt(String email, Money amount) {
        if (failing) {
            throw new NotificationException("SMTP indisponível", null);
        }
        sent.add(new Sent(email, amount));
    }

    public List<Sent> sent() {
        return sent;
    }
}
