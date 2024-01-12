package org.partiql.spi.fn

@RequiresOptIn(
    message = "PartiQLFunction requires explicit opt-in",
    level = RequiresOptIn.Level.ERROR,
)
public annotation class FnExperimental
