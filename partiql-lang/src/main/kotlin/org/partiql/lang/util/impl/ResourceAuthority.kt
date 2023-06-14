package org.partiql.lang.util.impl

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ionschema.Authority
import com.amazon.ionschema.IonSchemaSystem
import com.amazon.ionschema.util.CloseableIterator
import java.io.InputStream

internal class ResourceAuthority(
    private val rootPackage: String,
    private val classLoader: ClassLoader,
    val ion: IonSystem
) : Authority {

    companion object {
        internal fun getResourceAuthority(ion: IonSystem) =
            ResourceAuthority("org/partiql/schemas", ResourceAuthority::class.java.classLoader, ion)
    }

    override fun iteratorFor(iss: IonSchemaSystem, id: String): CloseableIterator<IonValue> {
        val resourceName = "$rootPackage/$id"
        val str: InputStream = classLoader.getResourceAsStream(resourceName)
            ?: error("Failed to load schema with resource name '$resourceName'")

        return object : CloseableIterator<IonValue> {

            private var stream = str
            private var reader = ion.newReader(stream).also { it.next() }
            private var iter = ion.iterate(reader)

            override fun hasNext() = iter.hasNext()
            override fun next() = iter.next()
            override fun close() {
                try {
                    reader?.close()
                    stream.close()
                } finally {
                    reader = null
                }
            }
        }
    }
}
