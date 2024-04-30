package org.partiql.beam.io.runtime

import com.amazon.ion.IonReader
import com.amazon.ion.IonType

public abstract class RidlReader(private val reader: IonReader) {

    public fun assertTag(expectedType: IonType, expectedTag: String) {
        assert(reader.type == IonType.STRUCT) { "Expected `$expectedType`, got `${reader.type}`" }
        val tag = reader.typeAnnotations.joinToString("::")
        assert(tag == expectedTag) { "Expected `$expectedTag`, got `$tag`" }
    }

    public fun assertKey(key: String) {
        assert(reader.fieldName == key) { "Expected field `$key`, found `$reader.fieldName`" }
    }
}
