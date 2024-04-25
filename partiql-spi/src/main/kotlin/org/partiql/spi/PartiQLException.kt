package org.partiql.spi

public class PartiQLException(
    override val message: String
) : RuntimeException()
