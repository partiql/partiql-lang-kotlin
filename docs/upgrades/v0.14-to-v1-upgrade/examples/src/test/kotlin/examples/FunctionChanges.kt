package examples

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.Int32Value
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.StringValue
import org.partiql.value.float64Value
import org.partiql.value.int32Value
import org.partiql.value.stringValue
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

class FunctionChanges {
    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun `defining function parameters`() {
        // In PLK 0.14.9, function parameters use FunctionParameter with PartiQLValueType
        // Creating parameters for function definition
        val stringParam = FunctionParameter("text", PartiQLValueType.STRING)
        val intParam = FunctionParameter("count", PartiQLValueType.INT)
        assertEquals("text", stringParam.name) // Direct property access
        assertEquals(PartiQLValueType.INT, intParam.type) // Direct property access
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun `creating function signatures`() {
        // In PLK 0.14.9, function signatures use FunctionSignature.Scalar for a PartiQL scalar function
        // and FunctionSignature.Aggregation for a PartiQL aggregation function.
        val sig1 = FunctionSignature.Scalar(
            name = "scalar",
            returns = PartiQLValueType.BOOL,
            parameters = listOf(
                FunctionParameter("x1", PartiQLValueType.INT),
                FunctionParameter("y1", PartiQLValueType.STRING)
            ),
            description = "this is a scalar function", // Default null
            isNullCall = false, // Flag indicating if any of the call arguments is NULL, then return NULL
            // v0.14.9 specific property
            isDeterministic = true // Flag indicating this function always produces the same output given the same input
        )
        assertEquals(2, sig1.parameters.size)
        assertEquals("x1", sig1.parameters[0].name)
        // v0.14.9 has symbolic name for debugging/identification
        assertEquals("SCALAR__INT_STRING__BOOL", sig1.specific)
        // toString() uses specific
        assertEquals("SCALAR__INT_STRING__BOOL", sig1.toString())
        // In v0.14.9, Scalar signatures can generate SQL CREATE FUNCTION syntax
        val sql = sig1.sql()
        assertEquals(true, sql.contains("CREATE FUNCTION"))
        val sig2 = FunctionSignature.Scalar(
            name = "scalar",
            returns = PartiQLValueType.BOOL,
            parameters = listOf(
                FunctionParameter("x2", PartiQLValueType.INT),
                FunctionParameter("y2", PartiQLValueType.STRING)
            ),
            isNullCall = false,
            isDeterministic = true
        )
        // v0.14.9 has custom equals() ignoring param names and description
        assertEquals(true, sig1 == sig2)
        // hashCode() does not ignores description
        assertEquals(false, sig1.hashCode() == sig2.hashCode())
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun `comparing signatures vs overload signature`() {
        // In PLK 0.14.9, there is NO dedicated class for overload management
        // Function overloading was handled differently through the function signature system
        val overload1 = FunctionSignature.Scalar(
            "func", PartiQLValueType.STRING,
            listOf(FunctionParameter("x", PartiQLValueType.INT))
        )
        val overload2 = FunctionSignature.Scalar(
            "func", PartiQLValueType.STRING,
            listOf(FunctionParameter("x", PartiQLValueType.STRING))
        )
        // Same function name which means different parameter types are different overloads
        assertEquals(false, overload1 == overload2)
    }

    @OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
    @Test
    fun `implementing scalar functions`() {
        // In PLK 0.14.9, scalar functions implement PartiQLFunction.Scalar interface
        // with PartiQLValueType
        class UpperFunction : PartiQLFunction.Scalar {
            override val signature = FunctionSignature.Scalar(
                "upper",
                PartiQLValueType.STRING,
                listOf(FunctionParameter("input", PartiQLValueType.STRING))
            )

            override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
                val input = (args[0] as StringValue).value!!
                return stringValue(input.uppercase())
            }
        }

        val upperFunc = UpperFunction()
        assertEquals(1, upperFunc.signature.parameters.size)
        // Function can be invoked directly
        val result = upperFunc.invoke(arrayOf(stringValue("hello")))
        assertEquals("HELLO", (result as StringValue).value!!)
    }

    @OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
    @Test
    fun `implementing aggregation functions`() {
        // In PLK 0.14.9, aggregation functions implement PartiQLFunction.Aggregation interface
        // with PartiQLValueType
        class Count : PartiQLFunction.Aggregation {
            // Check details in fun `creating function signatures`()
            override val signature = FunctionSignature.Aggregation(
                "count",
                PartiQLValueType.INT,
                emptyList()
            )

            // In PLK 0.14.9, Accumulator is a nested interface inside PartiQLFunction
            override fun accumulator(): PartiQLFunction.Accumulator {
                return object : PartiQLFunction.Accumulator {
                    private var count = 0

                    override fun next(args: Array<PartiQLValue>): PartiQLValue {
                        count++
                        return int32Value(count)
                    }

                    override fun value(): PartiQLValue {
                        return int32Value(count)
                    }
                }
            }
        }

        val countFunc = Count()
        val acc = countFunc.accumulator()
        acc.next(emptyArray())
        acc.next(emptyArray())
        assertEquals(int32Value(2), acc.value())
    }

    @OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
    @Test
    fun `working with function overloads`() {
        // In PLK 0.14.9, there are NO dedicated FnOverload or AggOverload classes
        // Function overloading is handled by having multiple PartiQLFunction implementations
        // with different signatures but the same name
        // Example: Multiple ABS functions for different types
        // Abs for INT
        class AbsIntOverload : PartiQLFunction.Scalar {
            override val signature = FunctionSignature.Scalar(
                "abs", PartiQLValueType.INT,
                listOf(FunctionParameter("x", PartiQLValueType.INT))
            )
            override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
                val value = (args[0] as Int32Value).value!!
                return int32Value(abs(value))
            }
        }

        // Abs for Double
        class AbsDoubleOverload : PartiQLFunction.Scalar {
            override val signature = FunctionSignature.Scalar(
                "abs", PartiQLValueType.FLOAT64,
                listOf(FunctionParameter("x", PartiQLValueType.FLOAT64))
            )
            override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
                val value = (args[0] as org.partiql.value.Float64Value).value!!
                return float64Value(abs(value))
            }
        }

        val absIntOverload = AbsIntOverload()
        val absDoubleOverload = AbsDoubleOverload()
        // Same function name, different parameter types are overloads
        // Different implementations for different types
        val intResult = absIntOverload.invoke(arrayOf(int32Value(-5)))
        val doubleResult = absDoubleOverload.invoke(arrayOf(float64Value(-3.14)))
        assertEquals(int32Value(5), intResult)
        assertEquals(float64Value(3.14), doubleResult)
    }
}
