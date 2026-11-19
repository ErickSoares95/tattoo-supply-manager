package com.ericksoares.tattoo.user;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725", "111.444.777-35"})
    void acceptsValidCpfs(String cpf) {
        assertTrue(validator.isValid(cpf, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-24", "12345678900", "111.111.111-11", "00000000000", "5299822472"})
    void rejectsInvalidCpfs(String cpf) {
        assertFalse(validator.isValid(cpf, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void cpfIsOptional(String cpf) {
        assertTrue(validator.isValid(cpf, null));
    }
}
