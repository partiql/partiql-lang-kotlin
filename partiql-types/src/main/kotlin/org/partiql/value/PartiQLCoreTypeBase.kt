package org.partiql.value

@Deprecated("How can I make this internal?")
public abstract class PartiQLCoreTypeBase : PartiQLType.Runtime.Core {
    override fun toString(): String = this.name
}
