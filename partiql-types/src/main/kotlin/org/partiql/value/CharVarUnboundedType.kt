package org.partiql.value

/**
 * SQL:1999's CHARACTER VARYING type
 * Aliases are VARCHAR, STRING, and SYMBOL (both are unbounded in length)
 */
public object CharVarUnboundedType : PartiQLCoreTypeBase() {
    override val name: String = "STRING" // TODO: For now
}
