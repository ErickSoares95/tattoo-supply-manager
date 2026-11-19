package com.ericksoares.tattoo.payment;

/** Y3: the methods the storefront offers; anything else is a 400 (and a CHECK in V4). */
enum PaymentMethod {
    PIX,
    CREDIT_CARD,
    DEBIT_CARD,
    BOLETO
}
