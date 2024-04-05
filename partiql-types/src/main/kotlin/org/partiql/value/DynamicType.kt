package org.partiql.value

public object DynamicType : PartiQLType.Abstract {
    override val name: String = "DYNAMIC"
    override fun toString(): String = this.name
}
