package com.amazon.ionsql.eval.builtins

import com.amazon.ion.system.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*
import org.junit.*
import org.junit.Assert.*

class SubstringExprFunctionTest {
    private val ion = IonSystemBuilder.standard().build()
    private val subject = SubstringExprFunction(ion)

    private val env = Environment(locals = Bindings.empty(),
                                  session = EvaluationSession.standard(),
                                  registers = RegisterBank(0))

    private fun execSubstring(str: String, startIndex: Int): String {
        val partOfString = subject.call(env,
                                          listOf(ion.newString(str).exprValue(),
                                                 ion.newInt(startIndex).exprValue()))

        return partOfString.ionValue.stringValue()!!
    }

    private fun execSubstring(str: String, startIndex: Int, length: Int): String {
        val partOfString = subject.call(env,
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

    @Test
    fun substringWithNegativeLength() = try {
        execSubstring("abcdefg", 3, -1)
        fail("didn't throw")
    } catch (e: EvaluationException) {
        assertEquals("Invalid start position or length arguments to substring function.", e.message)
        assertEquals(false, e.internal)
    }

    @Test
    fun substringWithNonIntSecondArgument() = try {
        subject.call(env,
                     listOf(ion.newString("1234567890").exprValue(),
                            ion.newDecimal(3).exprValue(),
                            ion.newInt(2).exprValue()))
        fail("didn't throw")
    } catch (e: EvaluationException) {
        assertEquals("Argument 2 of substring was not INT.", e.message)
        assertEquals(false, e.internal)
    }

    @Test
    fun substringWithNonIntThirdArgument() = try {
        subject.call(env,
                     listOf(ion.newString("1234567890").exprValue(),
                            ion.newInt(3).exprValue(),
                            ion.newDecimal(2).exprValue()))
        fail("didn't throw")
    } catch (e: EvaluationException) {
        assertEquals("Argument 3 of substring was not INT.", e.message)
        assertEquals(false, e.internal)
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
        var partOfString = subject.call(env,
                                          listOf(
                                              ion.newNull().exprValue(),
                                              ion.newInt(3).exprValue(),
                                              ion.newInt(2).exprValue()))
        assertEquals(ion.newNull(), partOfString.ionValue)

        //Second parameter is null
        partOfString = subject.call(env,
                                      listOf(
                                          ion.newString("abcdefg").exprValue(),
                                          ion.newNull().exprValue(),
                                          ion.newInt(2).exprValue()))
        assertEquals(ion.newNull(), partOfString.ionValue)

        //Third parameter is null
        partOfString = subject.call(env,
                                      listOf(
                                          ion.newString("abcdefg").exprValue(),
                                          ion.newInt(3).exprValue(),
                                          ion.newNull().exprValue()))
        assertEquals(ion.newNull(), partOfString.ionValue)
    }

}