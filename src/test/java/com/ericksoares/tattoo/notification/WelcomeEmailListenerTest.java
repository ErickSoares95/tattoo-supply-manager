package com.ericksoares.tattoo.notification;

import com.ericksoares.tattoo.notification.sender.EmailNotificationSender;
import com.ericksoares.tattoo.user.UserRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WelcomeEmailListenerTest {

    @Mock
    private EmailNotificationSender emailSender;

    @InjectMocks
    private WelcomeEmailListener listener;

    @Test
    void shouldSendWelcomeEmail() {

        UserRegisteredEvent event = new UserRegisteredEvent(1L, "new@user.com", "New User", LocalDateTime.now());

        listener.handle(event);

        verify(emailSender).sendWelcomeEmail("new@user.com", "New User");
    }

    @Test
    void shouldNotPropagateExceptionWhenSenderFails() {

        UserRegisteredEvent event = new UserRegisteredEvent(1L, "new@user.com", "New User", LocalDateTime.now());

        doThrow(new RuntimeException("smtp indisponivel"))
                .when(emailSender).sendWelcomeEmail(any(), any());

        listener.handle(event);

        verify(emailSender).sendWelcomeEmail("new@user.com", "New User");
    }
}
