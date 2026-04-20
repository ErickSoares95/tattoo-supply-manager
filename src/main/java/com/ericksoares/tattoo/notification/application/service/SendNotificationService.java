package com.ericksoares.tattoo.notification.application.service;

import org.springframework.stereotype.Service;

@Service
public class SendNotificationService {

    public void sendOrderRegistered(String message) {
        System.out.println("📧 Sending notification: " + message);
    }
}