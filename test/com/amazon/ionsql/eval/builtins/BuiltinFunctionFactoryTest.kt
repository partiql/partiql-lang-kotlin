package com.amazon.ionsql.eval.builtins

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionsql.eval.Bindings
import com.amazon.ionsql.eval.Environment
import com.amazon.ionsql.eval.EvaluationException
import com.amazon.ionsql.eval.RegisterBank
import com.amazon.ionsql.util.booleanValue
import com.amazon.ionsql.util.exprValue
import com.amazon.ionsql.util.stringValue
import org.junit.Assert
import org.junit.Test

class BuiltinFunctionFactoryTest {
    val ion = IonSystemBuilder.standard().build()
    val bindings = Bindings.empty();
    val env = Environment(bindings, bindings, bindings, RegisterBank(0))
    val factory = BuiltinFunctionFactory(ion)

    @Test
    fun existsWithNonEmptyCollectionReturnsTrue() {
        val value = factory.exists().call(env, listOf(ion.newList(ion.newInt(1)).exprValue()))
        Assert.assertTrue(value.ionValue.booleanValue()!!)
    }

    @Test
    fun existsWithEmptyCollectionReturnsFalse() {
        val value = factory.exists().call(env, listOf(ion.newList().exprValue()))
        Assert.assertFalse(value.ionValue.booleanValue()!!)
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
        Assert.assertEquals("cdefg", execSubstring("abcdefg", 3))
    }

    @Test
    fun substringWithLength() {
        Assert.assertEquals("cd", execSubstring("abcdefg", 3, 2))
    }

    @Test(expected = EvaluationException::class)
    fun substringWithNegativeLength() {
        execSubstring("abcdefg", 3, -1)
    }

    @Test
    fun substringWithNegativeStartIndex() {
        Assert.assertEquals("abcdefg", execSubstring("abcdefg", -1))
    }

    @Test
    fun substringWithNegativeStartIndexAndLengthh() {
        Assert.assertEquals("ab", execSubstring("abcdefg", -1, 4))
    }

    @Test
    fun substringWithANullArgumentReturnsNull() {
        //First parameter is null
        var partOfString = factory.substring().call(env,
                listOf(
                        ion.newNull().exprValue(),
                        ion.newInt(3).exprValue(),
                        ion.newInt(2).exprValue()))
        Assert.assertEquals(ion.newNull(), partOfString.ionValue)

        //Second parameter is null
        partOfString = factory.substring().call(env,
                listOf(
                        ion.newString("abcdefg").exprValue(),
                        ion.newNull().exprValue(),
                        ion.newInt(2).exprValue()))
        Assert.assertEquals(ion.newNull(), partOfString.ionValue)

        //Third parameter is null
        partOfString = factory.substring().call(env,
                listOf(
                        ion.newString("abcdefg").exprValue(),
                        ion.newInt(3).exprValue(),
                        ion.newNull().exprValue()))
        Assert.assertEquals(ion.newNull(), partOfString.ionValue)
    }


    //TODO:  mysterious error condition in spec
}