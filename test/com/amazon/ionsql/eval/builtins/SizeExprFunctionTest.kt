package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ion.system.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*
import org.junit.Assert.*
import org.junit.Test

class SizeExprFunctionTest {
    private val ion = IonSystemBuilder.standard().build()
    private val subject = SizeExprFunction(ion)

    private val env = Environment(locals = Bindings.empty(),
                                  session = EvaluationSession.standard(),
                                  registers = RegisterBank(0))

    private fun callSize(vararg arg: ExprValue) = subject.call(env, arg.toList()).scalar.numberValue()

    private fun ionListOf(vararg values: IonValue): ExprValue {
        val list = ion.newEmptyList()
        values.forEach { list.add(it) }

        return list.exprValue()
    }

    private fun ionStructOf(vararg values: IonValue): ExprValue {
        val struct = ion.newEmptyStruct()
        values.forEachIndexed { index, value -> struct.put(index.toString(), value) }

        return struct.exprValue()
    }

    private fun ionDatagramOf(vararg values: IonValue): ExprValue {
        val datagram = ion.newDatagram()
        values.forEach { datagram.add(it) }

        return datagram.exprValue()
    }

    @Test
    fun emptyStruct() = assertEquals(0L, callSize(ionStructOf()))

    @Test
    fun emptyList() = assertEquals(0L, callSize(ionListOf()))

    @Test
    fun emptyBag() = assertEquals(0L, callSize(ionDatagramOf()))

    @Test
    fun singleElementStruct() = assertEquals(1L, callSize(ionStructOf(ion.newString("foo"))))

    @Test
    fun singleElementList() = assertEquals(1L, callSize(ionListOf(ion.newString("foo"))))

    @Test
    fun singleElementBag() = assertEquals(1L, callSize(ionDatagramOf(ion.newString("foo"))))

    @Test
    fun multiElementStruct() = assertEquals(2L, callSize(ionStructOf(ion.newString("foo"), ion.newString("bar"))))

    @Test
    fun multiElementList() = assertEquals(2L, callSize(ionListOf(ion.newString("foo"), ion.newString("bar"))))

    @Test
    fun multiElementBag() = assertEquals(2L, callSize(ionDatagramOf(ion.newString("foo"), ion.newString("bar"))))

    @Test
    fun wrongType() = try {
        callSize(ion.newInt(1).exprValue())
        fail("didn't throw")
    }
    catch (e: EvaluationException) {
        softAssert {
            assertThat(e.message).isEqualTo("invalid argument type for size")
            assertThat(e.errorCode).isEqualTo(ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL)
            assertThat(e.errorContext).isNotNull

            val errorContext = e.errorContext!!
            assertThat(errorContext[Property.EXPECTED_ARGUMENT_TYPES]?.stringValue()).isEqualTo("LIST or BAG or STRUCT")
            assertThat(errorContext[Property.ACTUAL_ARGUMENT_TYPES]?.stringValue()).isEqualTo("INT")
            assertThat(errorContext[Property.FUNCTION_NAME]?.stringValue()).isEqualTo("size")
        }
    }

    @Test
    fun lessArguments() = try {
        callSize()
        fail("didn't throw")
    }
    catch (e: EvaluationException) {
        softAssert {
            assertThat(e.message).isEqualTo("size takes a single argument, received: 0")
            assertThat(e.errorCode).isEqualTo(ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL)
            assertThat(e.errorContext).isNotNull

            val errorContext = e.errorContext!!
            assertThat(errorContext[Property.EXPECTED_ARITY_MIN]?.integerValue()).isEqualTo(1)
            assertThat(errorContext[Property.EXPECTED_ARITY_MAX]?.integerValue()).isEqualTo(1)
        }
    }

    @Test
    fun moreArguments() = try {
        callSize(ionStructOf(), ionStructOf())
        fail("didn't throw")
    }
    catch (e: EvaluationException) {
        softAssert {
            assertThat(e.message).isEqualTo("size takes a single argument, received: 2")
            assertThat(e.errorCode).isEqualTo(ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL)
            assertThat(e.errorContext).isNotNull

            val errorContext = e.errorContext!!
            assertThat(errorContext[Property.EXPECTED_ARITY_MIN]?.integerValue()).isEqualTo(1)
            assertThat(errorContext[Property.EXPECTED_ARITY_MAX]?.integerValue()).isEqualTo(1)
        }
    }
}