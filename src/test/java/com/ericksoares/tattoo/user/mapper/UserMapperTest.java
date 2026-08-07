package com.ericksoares.tattoo.user.mapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    @Test
    void shouldStripPunctuationFromCpf() {
        assertEquals("11122233344", UserMapper.normalizeCpf("111.222.333-44"));
    }

    @Test
    void shouldKeepAlreadyDigitsOnlyCpfUnchanged() {
        assertEquals("11122233344", UserMapper.normalizeCpf("11122233344"));
    }

    @Test
    void shouldReturnNullWhenCpfIsNull() {
        assertNull(UserMapper.normalizeCpf(null));
    }

    @Test
    void shouldReturnNullWhenCpfIsBlank() {
        assertNull(UserMapper.normalizeCpf("   "));
    }
}
