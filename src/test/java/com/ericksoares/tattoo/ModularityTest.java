package com.ericksoares.tattoo;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Guards the module boundaries: fails the build when a module reaches into another module's
 * internals (repositories, entities) or when modules form a dependency cycle. Cross-module
 * side effects go through events; queries and synchronous commands go through the owning
 * module's public API.
 */
class ModularityTest {

    @Test
    void verifiesModuleBoundaries() {
        ApplicationModules.of(TattooSupplyManagerApplication.class).verify();
    }
}
