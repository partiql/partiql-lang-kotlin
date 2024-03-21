package org.partiql.value

/**
 * SQL:1999's CHARACTER LARGE OBJECT type and Ion's CLOB type
 * Aliases are CLOB
 */
public object ClobUnboundedType : PartiQLCoreTypeBase() {
    override val name: String = "CLOB"
}
