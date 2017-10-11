package com.amazon.ionsql.eval.builtins

import com.amazon.ion.system.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*
import org.junit.*
import org.junit.Assert.*

class BuiltinFunctionFactoryTest {
    private val ion = IonSystemBuilder.standard().build()
    private val bindings = Bindings.empty();
    private val env = Environment(bindings, bindings, bindings, RegisterBank(0))
    private val factory = BuiltinFunctionFactory(ion)

    @Test
    fun existsWithNonEmptyCollectionReturnsTrue() {
        val value = factory.exists().call(env, listOf(ion.newList(ion.newInt(1)).exprValue()))
        assertTrue(value.ionValue.booleanValue()!!)
    }

    @Test
    fun existsWithEmptyCollectionReturnsFalse() {
        val value = factory.exists().call(env, listOf(ion.newList().exprValue()))
        assertFalse(value.ionValue.booleanValue()!!)
    }

    fun execSubstring(str: String, startIndex: Int): String {
        val partOfString = factory.substring().call(env,
                listOf(ion.newString(str).exprValue(),
                        ion.newInt(startIndex).exprValue()))

        return partOfString.ionValue.stringValue()!!
    }

    fun execSubstring(str: String, startIndex: Int, length: Int): String {
        val partOfString = factory.substring().call(env,
                listOf(ion.newString(str).exprValue(),
                        ion.newInt(startIndex).exprValue(),
                        ion.newInt(length).exprValue()))

        return partOfString.ionValue.stringValue()!!
    }

    @Test
    fun substringWithoutLength() {
        assertEquals("cdefg", execSubstring("abcdefg", 3))
    }

    @Test
    fun substringWithLength() {
        assertEquals("cd", execSubstring("abcdefg", 3, 2))
    }

    @Test(expected = EvaluationException::class)
    fun substringWithNegativeLength() {
        execSubstring("abcdefg", 3, -1)
    }

    @Test
    fun substringWithNegativeStartIndex() {
        assertEquals("abcdefg", execSubstring("abcdefg", -1))
    }

    @Test
    fun substringWithNegativeStartIndexAndLength() {
        assertEquals("ab", execSubstring("abcdefg", -1, 4))
    }

    @Test
    fun substringWithANullArgumentReturnsNull() {
        //First parameter is null
        var partOfString = factory.substring().call(env,
                listOf(
                        ion.newNull().exprValue(),
                        ion.newInt(3).exprValue(),
                        ion.newInt(2).exprValue()))
        assertEquals(ion.newNull(), partOfString.ionValue)

        //Second parameter is null
        partOfString = factory.substring().call(env,
                listOf(
                        ion.newString("abcdefg").exprValue(),
                        ion.newNull().exprValue(),
                        ion.newInt(2).exprValue()))
        assertEquals(ion.newNull(), partOfString.ionValue)

        //Third parameter is null
        partOfString = factory.substring().call(env,
                listOf(
                        ion.newString("abcdefg").exprValue(),
                        ion.newInt(3).exprValue(),
                        ion.newNull().exprValue()))
        assertEquals(ion.newNull(), partOfString.ionValue)
    }
}