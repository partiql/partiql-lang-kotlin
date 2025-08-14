package examples

import org.partiql.spi.function.Accumulator
import org.partiql.spi.function.Agg
import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.RoutineSignature
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

class FunctionChanges {
    @Test
    fun `defining function parameters`() {
        // In PLK v1, function parameters use Parameter class with PType
        // Creating parameters for function definition
        val stringParam = Parameter("text", PType.string())
        val intParam = Parameter("count", PType.integer())
        assertEquals("text", stringParam.name) // Looks like property, actually calls getName() getter
        assertEquals(PType.integer(), intParam.type) // Looks like property, actually calls getType() getter
    }

    @Test
    fun `creating function signatures`() {
        // In PLK v1, function signatures use RoutineSignature for both scalar and aggregation functions
        val scalarSignature1 = RoutineSignature(
            "scalar",
            listOf(
                Parameter("x1", PType.integer()),
                Parameter("y1", PType.string())
            ),
            PType.bool()
            // No description parameter available
            // isMissingCall and isNullCall default to true
        )
        assertEquals(2, scalarSignature1.arity) // v1.2.2 uses arity instead of parameters.size
        assertEquals("x1", scalarSignature1.parameters[0].name)
        // v1 has NO symbolic name which means toString() shows object reference
        assertEquals(true, scalarSignature1.toString().contains("RoutineSignature@"))
        // v1 has NO sql() method for generating SQL CREATE FUNCTION syntax
        val scalarSignature2 = RoutineSignature(
            "scalar",
            listOf(
                Parameter("x2", PType.integer()),
                Parameter("y2", PType.string())
            ),
            PType.bool()
        )
        // v1 uses default equals() (reference equality) which means different objects are NOT equal
        assertEquals(false, scalarSignature1 == scalarSignature2)
        // Different hash codes for different objects
        assertEquals(false, scalarSignature1.hashCode() == scalarSignature2.hashCode())
    }

    @Test
    fun `comparing signatures vs overload signature`() {
        // In PLK v1, RoutineOverloadSignature represents the signature of a specific overload
        // This is NEW in v1
        // Key differences from RoutineSignature:
        // 1. No return type (overload selection happens before knowing return type)
        // 2. No isNullCall/isMissingCall flags
        // 3. Only parameter types without names
        // RoutineSignature: Full signature with everything
        val fullSignature = RoutineSignature(
            "func",
            listOf(Parameter("x", PType.integer())), // With parameter name
            PType.string() // With return type
        )
        assertEquals(true, fullSignature.isNullCall)
        // RoutineOverloadSignature: Minimal signature for overload matching
        val overloadSignature = RoutineOverloadSignature(
            "func",
            listOf(PType.integer())
        )
        assertEquals(PType.integer(), overloadSignature.parameterTypes[0]) // Only parameter type
    }

    @Test
    fun `working with accumulators`() {
        // In PLK v1, accumulator is a public interface works with Datum arrays
        // Accumulator interface has:
        // 1.next(args: Datum[]): processes each set of arguments
        // 2.value(): Datum: returns final result
        // Sum accumulator example:
        class Sum : Accumulator {
            private var sum = 0

            override fun next(args: Array<Datum>) {
                // Possible process for args
                if (args.isNotEmpty()) {
                    val value = args[0]
                    if (!value.isNull) {
                        sum += value.int
                    }
                }
            }

            override fun value(): Datum {
                return Datum.integer(sum)
            }
        }

        val accumulator = Sum()
        accumulator.next(arrayOf(Datum.integer(1)))
        accumulator.next(arrayOf(Datum.integer(2)))
        val result = accumulator.value()
        assertEquals(3, result.int)
    }

    @Test
    fun `implementing scalar functions`() {
        // In PLK v1, scalar functions extend Fn abstract class with Datum
        val upperFunc = Fn.Builder("upper")
            .addParameter(Parameter("input", PType.string()))
            .isNullCall(true)
            .isMissingCall(true)
            .returns(PType.string())
            .body { args ->
                val input = args[0].string // We can safely get string here by .getString()
                Datum.string(input.uppercase())
            }
            .build()

        assertEquals(1, upperFunc.signature.arity)
        // Function can be invoked directly
        val result = upperFunc.invoke(arrayOf(Datum.string("hello")))
        assertEquals("HELLO", result.string)
    }

    @Test
    fun `implementing aggregation functions`() {
        // In PLK v1, aggregation functions extend Agg abstract class with Datum
        class Count : Agg() {
            // Check details in fun `creating function signatures`()
            override fun getSignature(): RoutineSignature {
                return RoutineSignature(
                    "count",
                    emptyList(),
                    PType.integer()
                )
            }

            // Check details in fun `working with accumulators`()
            override fun getAccumulator(): Accumulator {
                return object : Accumulator {
                    private var count = 0

                    override fun next(args: Array<Datum>) {
                        count++
                    }

                    override fun value(): Datum {
                        return Datum.integer(count)
                    }
                }
            }
        }

        val countFunc = Count()
        val acc = countFunc.accumulator
        acc.next(emptyArray())
        acc.next(emptyArray())
        assertEquals(2, acc.value().int)
    }

    @Test
    fun `working with function overloads`() {
        // In PLK v1, function overloading is handled by FnOverload and AggOverload classes
        // The recommended approach is to use FnOverload.Builder multiple times for different types
        // Create multiple overloads for the same function name with different parameter types
        val absIntOverload = FnOverload.Builder("abs")
            .addParameter(PType.integer())
            .returns(PType.integer())
            .body { args ->
                val value = args[0].int
                Datum.integer(abs(value))
            }
            .build()

        val absDoubleOverload = FnOverload.Builder("abs")
            .addParameter(PType.doublePrecision())
            .returns(PType.doublePrecision())
            .body { args ->
                val value = args[0].double
                Datum.doublePrecision(abs(value))
            }
            .build()

        // Simulate catalog storage (in real implementation, this would be in your catalog class)
        val functionOverloads = mapOf("abs" to listOf(absIntOverload, absDoubleOverload))
        // Get all Abs overloads
        val absOverloads = functionOverloads["abs"]!!
        // Get instances for specific argument types
        val absIntFn = absOverloads[0].getInstance(arrayOf(PType.integer()))
        val absDoubleFn = absOverloads[1].getInstance(arrayOf(PType.doublePrecision()))
        // Test different overloads with their respective types
        val intResult = absIntFn!!.invoke(arrayOf(Datum.integer(-5)))
        val doubleResult = absDoubleFn!!.invoke(arrayOf(Datum.doublePrecision(-3.14)))
        assertEquals(5, intResult.int)
        assertEquals(3.14, doubleResult.double)
    }
}
