package org.partiql.sprout.parser.ion

import com.amazon.ion.IonList
import com.amazon.ion.IonStruct
import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Test

class IonExtensionsKtTest {

    @Test
    fun isContainer() {
        val ion = IonSystemBuilder.standard().build()
        val a = ion.loader.load("{ _:[] }")[0] as IonStruct
        val b = ion.loader.load("[ _::[] ]")[0] as IonList
        assert(a.first().isContainer())
        assert(b.first().isContainer())
    }
}
