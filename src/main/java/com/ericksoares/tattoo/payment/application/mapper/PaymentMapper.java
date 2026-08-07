package com.ericksoares.tattoo.payment.application.mapper;

import com.ericksoares.tattoo.payment.application.dto.PaymentResponse;
import com.ericksoares.tattoo.payment.domain.entity.Payment;

public class PaymentMapper {

    private PaymentMapper() {}

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getCreationDate()
        );
    }
}
