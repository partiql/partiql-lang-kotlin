package org.partiql.value

import java.io.InputStream

public interface PartiQLValueReader : AutoCloseable {

    public fun readValue(input: InputStream): PartiQLValue

    public fun readValues(input: InputStream): Iterator<PartiQLValue>
}
