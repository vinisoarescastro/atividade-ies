package com.saas.billing.adapter.payment;

import com.saas.billing.domain.Money;
import com.saas.billing.port.PaymentGateway;

/**
 * Gateway simulado que reproduz a regra do código legado: todo token
 * iniciado por "tok_" é aprovado. Deve ser substituído pelo adaptador
 * do gateway real (Stripe, Pagar.me etc.) sem alterar o serviço.
 */
public class TokenPrefixPaymentGateway implements PaymentGateway {

    private static final String VALID_TOKEN_PREFIX = "tok_";

    @Override
    public boolean charge(String cardToken, Money amount) {
        return cardToken != null && cardToken.startsWith(VALID_TOKEN_PREFIX);
    }
}
