package com.ericksoares.tattoo.product.domain.enums;

// Values in English (same convention as user.domain.enums.UserType/UserStatus) - the
// storefront's department menu (Máquinas/Agulhas/Tintas/...) maps these to Portuguese
// labels client-side, same as it already does for UserType badges in the admin panel.
public enum ProductCategory {
    MACHINES,
    NEEDLES,
    INKS,
    DISPOSABLES,
    AFTERCARE,
    ACCESSORIES
}
