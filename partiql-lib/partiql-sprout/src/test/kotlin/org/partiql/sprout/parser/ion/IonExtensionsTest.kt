package org.partiql.sprout.parser.ion

import com.amazon.ion.IonList
import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse

class IonExtensionsTest {

    private val ion = IonSystemBuilder.standard().build()

    @Test
    internal fun testIsEnum() {
        val a = ion.singleValue("[ a, b, c, ]") as IonList
        val b = ion.singleValue("[ A, B, C, ]") as IonList
        val c = ion.singleValue("[ A_X, B_Y, C_Z, ]") as IonList
        val d = ion.singleValue("[ ABC_DEF_1, X_123, Y456 ]") as IonList
        assertFalse(a.isEnum())
        assert(b.isEnum())
        assert(c.isEnum())
        assert(d.isEnum())
    }
}
