package com.saas.billing.port;

/** Falha ao notificar o cliente. Não invalida uma cobrança já registrada. */
public class NotificationException extends InfrastructureException {

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
