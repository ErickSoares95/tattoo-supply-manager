package com.ericksoares.tattoo.product;

// Values in English (same convention as user.UserType/UserStatus) - the
// storefront's department menu (Máquinas/Agulhas/Tintas/...) maps these to Portuguese
// labels client-side, same as it already does for UserType badges in the admin panel.
enum ProductCategory {
    MACHINES,
    NEEDLES,
    INKS,
    DISPOSABLES,
    AFTERCARE,
    ACCESSORIES
}
