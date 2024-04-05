package org.partiql.value

/**
 * SQL:1999's DATE type
 * TODO: Does this differ from Ion?
 */
public object DateType : PartiQLCoreTypeBase() {
    override val name: String = "DATE"
}
