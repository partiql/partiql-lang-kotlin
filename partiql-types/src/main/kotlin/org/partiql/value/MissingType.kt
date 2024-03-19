package org.partiql.value

public object MissingType : PartiQLType.Runtime.Core {
    override val name: String = "MISSING"
    override fun toString(): String = this.name
}
