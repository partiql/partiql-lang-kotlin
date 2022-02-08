package org.partiql.lang.eval.builtins

import com.amazon.ion.IonValue
import org.junit.Test
import org.partiql.lang.TestBase
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.RequiredArgs
import org.partiql.lang.eval.call

class SizeExprFunctionTest : TestBase() {
    private val subject = SizeExprFunction(valueFactory)

    private val env = Environment.standard()

    private fun callSize(vararg arg: ExprValue): Number? = subject.call(env, RequiredArgs(arg.toList())).scalar.numberValue()

    private fun listOf(vararg values: IonValue): ExprValue = valueFactory.newList(
        values.asSequence().map { valueFactory.newFromIonValue(it) })

    private fun structOf(vararg values: IonValue): ExprValue {
        val struct = ion.newEmptyStruct()
        values.forEachIndexed { index, value -> struct.put(index.toString(), value) }

        return valueFactory.newFromIonValue(struct)
    }

    private fun bagOf(vararg values: IonValue): ExprValue {
        return valueFactory.newBag(values.asSequence().map { valueFactory.newFromIonValue(it) })
    }

    @Test
    fun emptyStruct() = assertEquals(0L, callSize(structOf()))

    @Test
    fun emptyList() = assertEquals(0L, callSize(listOf()))

    @Test
    fun emptyBag() = assertEquals(0L, callSize(bagOf()))

    @Test
    fun singleElementStruct() = assertEquals(1L, callSize(structOf(ion.newString("foo"))))

    @Test
    fun singleElementList() = assertEquals(1L, callSize(listOf(ion.newString("foo"))))

    @Test
    fun singleElementBag() = assertEquals(1L, callSize(bagOf(ion.newString("foo"))))

    @Test
    fun multiElementStruct() = assertEquals(2L, callSize(structOf(ion.newString("foo"), ion.newString("bar"))))

    @Test
    fun multiElementList() = assertEquals(2L, callSize(listOf(ion.newString("foo"), ion.newString("bar"))))

    @Test
    fun multiElementBag() = assertEquals(2L, callSize(bagOf(ion.newString("foo"), ion.newString("bar"))))
}