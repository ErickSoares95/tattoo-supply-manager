package com.ericksoares.tattoo.notification;

import com.ericksoares.tattoo.notification.sender.NotificationSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSenderExecutorTest {

    @Mock
    private NotificationSender sender;

    private final NotificationSenderExecutor executor = new NotificationSenderExecutor();

    @Test
    void shouldAttemptDeliveryExactlyOnceEvenWhenItFails() {

        doThrow(new ResourceAccessException("timeout", new IOException("timeout")))
                .when(sender).sendOrderRegistered(1L);

        assertThrows(TransientNotificationException.class, () -> executor.send(sender, 1L));

        verify(sender, times(1)).sendOrderRegistered(1L);
    }

    @Test
    void shouldClassifyNetworkAndServerErrorsAsTransient() {

        assertInstanceOf(TransientNotificationException.class,
                NotificationErrorClassifier.classify(new IOException("connection reset")));
        assertInstanceOf(TransientNotificationException.class,
                NotificationErrorClassifier.classify(new ResourceAccessException("I/O error")));
        assertInstanceOf(TransientNotificationException.class,
                NotificationErrorClassifier.classify(new HttpServerErrorException(HttpStatus.BAD_GATEWAY)));
        assertInstanceOf(TransientNotificationException.class,
                NotificationErrorClassifier.classify(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS)));
        assertInstanceOf(TransientNotificationException.class,
                NotificationErrorClassifier.classify(new RuntimeException("unknown")));
    }

    @Test
    void shouldClassifyInvalidPayloadAndClientErrorsAsPermanent() {

        assertInstanceOf(PermanentNotificationException.class,
                NotificationErrorClassifier.classify(new HttpClientErrorException(HttpStatus.BAD_REQUEST)));
        assertInstanceOf(PermanentNotificationException.class,
                NotificationErrorClassifier.classify(new IllegalArgumentException("bad payload")));
    }

    @Test
    void shouldKeepAlreadyClassifiedExceptionsUntouched() {

        PermanentNotificationException permanent = new PermanentNotificationException("no");
        TransientNotificationException transientError = new TransientNotificationException("later", null);

        assertSame(permanent, NotificationErrorClassifier.classify(permanent));
        assertSame(transientError, NotificationErrorClassifier.classify(transientError));
    }

    @Test
    void shouldNotWrapSuccessfulDelivery() {

        assertDoesNotThrow(() -> executor.send(sender, 1L));

        verify(sender).sendOrderRegistered(1L);
    }
}
