package com.ericksoares.tattoo.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** N11: GET /notifications/failed lists unprocessed failures, optionally only the parked ones. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FailedNotificationsEndpointIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FailedNotificationRepository orderFailures;

    @Autowired
    private FailedPaymentNotificationRepository paymentFailures;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listsPendingAndParkedFailuresOfBothKinds() throws Exception {
        FailedNotification pending = new FailedNotification();
        pending.setOrderId(71_001L);
        pending.setSenderName("EMAIL");
        pending.setAttempts(1);
        pending.setNextAttemptAt(Instant.now().plusSeconds(60));
        pending.setErrorMessage("timeout");
        pending.setCreatedAt(LocalDateTime.now());
        orderFailures.save(pending);

        FailedPaymentNotification parked = new FailedPaymentNotification();
        parked.setOrderId(71_002L);
        parked.setStatus("APPROVED");
        parked.setSenderName("WEBHOOK");
        parked.setAttempts(NotificationRetryPolicy.MAX_ATTEMPTS);
        parked.setNextAttemptAt(null);
        parked.setErrorMessage("400 invalid payload");
        parked.setCreatedAt(LocalDateTime.now());
        paymentFailures.save(parked);

        mockMvc.perform(get("/notifications/failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].orderId", hasItem(71_001)))
                .andExpect(jsonPath("$[*].orderId", hasItem(71_002)));

        mockMvc.perform(get("/notifications/failed").param("parked", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].parked", everyItem(is(true))))
                .andExpect(jsonPath("$[*].orderId", hasItem(71_002)));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void isAdminOnly() throws Exception {
        mockMvc.perform(get("/notifications/failed")).andExpect(status().isForbidden());
    }
}
