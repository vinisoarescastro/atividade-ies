package com.saas.billing.port;

/** Falha de infraestrutura (banco, rede, SMTP) lançada pelos adaptadores. */
public class InfrastructureException extends RuntimeException {

    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
