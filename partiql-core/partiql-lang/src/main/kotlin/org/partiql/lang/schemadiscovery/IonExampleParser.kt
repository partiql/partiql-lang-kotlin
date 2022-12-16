package org.partiql.lang.schemadiscovery

import com.amazon.ion.IonReader
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue

/**
 * Basic parser for ion data.
 */
class IonExampleParser(val ion: IonSystem) {
    /**
     * Returns the next [IonValue] or null if there are no move values to read.
     */
    fun parseExample(reader: IonReader): IonValue? {
        reader.next() ?: return null
        return ion.newValue(reader)
    }
}
