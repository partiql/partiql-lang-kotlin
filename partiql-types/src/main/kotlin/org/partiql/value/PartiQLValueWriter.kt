package org.partiql.value

public interface PartiQLValueWriter {

    public fun writeValue(value: PartiQLValue)

    public fun writeValues(values: Iterator<PartiQLValue>)
}
