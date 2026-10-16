package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.product.InsufficientStockException;
import com.ericksoares.tattoo.product.StockConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Retry behaviour of the order entry point. The order logic itself (stock, validation, events)
 * is covered by {@link RegisterOrderTransactionTest}.
 */
@ExtendWith(MockitoExtension.class)
class RegisterOrderServiceTest {

    @Mock
    private RegisterOrderTransaction transaction;

    @InjectMocks
    private RegisterOrderService service;

    private final OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(1L, 1)));

    private ObjectOptimisticLockingFailureException conflict() {
        return new ObjectOptimisticLockingFailureException("Product", 1L);
    }

    @Test
    void shouldReturnResultWithoutRetryWhenFirstAttemptSucceeds() {

        OrderResponse response = mock(OrderResponse.class);
        when(transaction.execute(request, 1L)).thenReturn(response);

        assertSame(response, service.execute(request, 1L));

        verify(transaction, times(1)).execute(request, 1L);
    }

    @Test
    void shouldRetryTheWholeTransactionOnOptimisticLockFailure() {

        OrderResponse response = mock(OrderResponse.class);
        when(transaction.execute(request, 1L))
                .thenThrow(conflict())
                .thenThrow(conflict())
                .thenReturn(response);

        assertSame(response, service.execute(request, 1L));

        verify(transaction, times(3)).execute(request, 1L);
    }

    @Test
    void shouldMapToConflictWhenRetriesAreExhausted() {

        when(transaction.execute(request, 1L)).thenThrow(conflict());

        StockConflictException ex = assertThrows(
                StockConflictException.class,
                () -> service.execute(request, 1L)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(transaction, times(RegisterOrderService.MAX_ATTEMPTS)).execute(request, 1L);
    }

    @Test
    void shouldNotRetryBusinessErrors() {

        when(transaction.execute(request, 1L)).thenThrow(new InsufficientStockException("Tinta Black"));

        assertThrows(InsufficientStockException.class, () -> service.execute(request, 1L));

        verify(transaction, times(1)).execute(request, 1L);
    }
}
