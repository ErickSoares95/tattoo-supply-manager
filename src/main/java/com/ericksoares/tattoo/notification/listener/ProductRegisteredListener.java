package com.ericksoares.tattoo.notification.listener;

import com.ericksoares.tattoo.product.domain.event.ProductRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProductRegisteredListener {

    private static final Logger log = LoggerFactory.getLogger(ProductRegisteredListener.class);

    @EventListener
    public void handle(ProductRegisteredEvent event) {

        log.info("📢 Product created: ID={}, Name={}, Price={}",
                event.productId(),
                event.name(),
                event.price()
        );

        // enviar email
        // enviar kafka
        // webhook
    }
}