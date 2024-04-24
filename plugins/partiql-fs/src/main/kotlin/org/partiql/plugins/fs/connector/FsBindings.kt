package org.partiql.plugins.fs.connector

import org.partiql.plugins.fs.index.FsIndex
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorPath
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.io.PartiQLValueIonReaderBuilder
import org.partiql.value.nullValue

internal class FsBindings(private val index: FsIndex) : ConnectorBindings {

    @OptIn(PartiQLValueExperimental::class)
    override fun getValue(path: ConnectorPath): PartiQLValue {
        val steps = path.map { BindingName(it, BindingCase.SENSITIVE) }
        val match = index.search(BindingPath(steps))
        if (match == null) {
            return nullValue()
        }
        val file = match.first.data
        if (file == null) {
            return nullValue()
        }
        val reader = PartiQLValueIonReaderBuilder.standard().build(file.inputStream())
        val value = reader.read()
        reader.close()
        return value
    }
}
