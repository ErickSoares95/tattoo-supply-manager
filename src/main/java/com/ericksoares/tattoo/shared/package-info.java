/**
 * Cross-cutting infrastructure (security, base entity, exceptions, async config) used by every
 * module. OPEN: all of its types are accessible to other modules. It must not depend on any
 * business module.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.ericksoares.tattoo.shared;

import org.springframework.modulith.ApplicationModule;
