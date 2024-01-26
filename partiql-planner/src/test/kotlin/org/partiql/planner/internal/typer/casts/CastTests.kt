package org.partiql.planner.internal.typer.casts

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import java.util.stream.Stream

@OptIn(PartiQLValueExperimental::class)
class CastTests : CastTestBase() {

    @TestFactory
    @Disabled("Halt on CAST ... AS ANY for now")
    fun test_any(): Stream<DynamicContainer> {
        return super.testGen(StaticType.ANY, PartiQLValueType.ANY, "cast_any", 0)
    }

    @TestFactory
    @Disabled("Halt on CAST ... AS NULL for now.")
    fun test_null(): Stream<DynamicContainer> {
        return super.testGen(StaticType.NULL, PartiQLValueType.NULL, "cast_null", 1)
    }

    @TestFactory
    @Disabled("Halt on CAST ... AS MISSING for now.")
    fun test_missing(): Stream<DynamicContainer> {
        return super.testGen(StaticType.MISSING, PartiQLValueType.MISSING, "cast_missing", 2)
    }

    @TestFactory
    fun test_bool(): Stream<DynamicContainer> {
        return super.testGen(StaticType.BOOL, PartiQLValueType.BOOL, "cast_bool", 3)
    }

    @TestFactory
    fun test_int2(): Stream<DynamicContainer> {
        return super.testGen(StaticType.INT2, PartiQLValueType.INT16, "cast_int2", 4)
    }

    @TestFactory
    fun test_int4(): Stream<DynamicContainer> {
        return super.testGen(StaticType.INT4, PartiQLValueType.INT32, "cast_int4", 5)
    }

    @TestFactory
    fun test_int8(): Stream<DynamicContainer> {
        return super.testGen(StaticType.INT8, PartiQLValueType.INT64, "cast_int8", 6)
    }

    @TestFactory
    fun test_int(): Stream<DynamicContainer> {
        return super.testGen(StaticType.INT, PartiQLValueType.INT, "cast_int", 7)
    }
    @TestFactory
    fun test_float64(): Stream<DynamicContainer> {
        return super.testGen(StaticType.FLOAT, PartiQLValueType.FLOAT64, "cast_float64", 10)
    }

    @TestFactory
    fun test_decimal_arbitrary(): Stream<DynamicContainer> {
        return super.testGen(StaticType.DECIMAL, PartiQLValueType.DECIMAL_ARBITRARY, "cast_decimal_arbitrary", 11)
    }

    @TestFactory
    @Disabled("Char constraint lost during Value Type to Static Type Conversion")
    fun test_char(): Stream<DynamicContainer> {
        return super.testGen(StaticType.CHAR, PartiQLValueType.CHAR, "cast_char", 12)
    }

    @TestFactory
    fun test_varchar(): Stream<DynamicContainer> {
        return super.testGen(StaticType.STRING, PartiQLValueType.STRING, "cast_varchar", 14)
    }

    @TestFactory
    fun test_symbol(): Stream<DynamicContainer> {
        return super.testGen(StaticType.SYMBOL, PartiQLValueType.SYMBOL, "cast_symbol", 16)
    }

    @TestFactory
    @Disabled("Halt on CAST ... AS CLOB for now.")
    fun test_clob(): Stream<DynamicContainer> {
        return super.testGen(StaticType.CLOB, PartiQLValueType.CLOB, "cast_clob", 17)
    }

    @TestFactory
    @Disabled("Halt on CAST ... AS BLOB for now.")
    fun test_blob(): Stream<DynamicContainer> {
        return super.testGen(StaticType.BLOB, PartiQLValueType.BLOB, "cast_blob", 20)
    }

    @TestFactory
    fun test_date(): Stream<DynamicContainer> {
        return super.testGen(StaticType.DATE, PartiQLValueType.DATE, "cast_date", 21)
    }

    @TestFactory
    fun test_time(): Stream<DynamicContainer> {
        return super.testGen(StaticType.TIME, PartiQLValueType.TIME, "cast_time", 22)
    }

    @TestFactory
    fun test_list(): Stream<DynamicContainer> {
        return super.testGen(StaticType.LIST, PartiQLValueType.LIST, "cast_list", 31)
    }

    @TestFactory
    fun test_sexp(): Stream<DynamicContainer> {
        return super.testGen(StaticType.SEXP, PartiQLValueType.SEXP, "cast_sexp", 32)
    }

    @TestFactory
    fun test_struct(): Stream<DynamicContainer> {
        return super.testGen(StaticType.STRUCT, PartiQLValueType.STRUCT, "cast_struct", 33)
    }

    @TestFactory
    fun test_bag(): Stream<DynamicContainer> {
        return super.testGen(StaticType.BAG, PartiQLValueType.BAG, "cast_bag", 34)
    }
}

@OptIn(PartiQLValueExperimental::class)
open class CastTestBase() : PartiQLTyperTestBase() {

    private fun PartiQLValueType.valueTypeToStaticType() =
        when (this) {
            PartiQLValueType.ANY -> StaticType.ANY
            PartiQLValueType.BOOL -> StaticType.BOOL
            PartiQLValueType.INT8 -> null
            PartiQLValueType.INT16 -> StaticType.INT2
            PartiQLValueType.INT32 -> StaticType.INT4
            PartiQLValueType.INT64 -> StaticType.INT8
            PartiQLValueType.INT -> StaticType.INT
            PartiQLValueType.DECIMAL_ARBITRARY -> StaticType.DECIMAL
            PartiQLValueType.DECIMAL -> null
            PartiQLValueType.FLOAT32 -> StaticType.FLOAT
            PartiQLValueType.FLOAT64 -> StaticType.FLOAT
            PartiQLValueType.CHAR -> StaticType.CHAR
            PartiQLValueType.STRING -> StaticType.STRING
            PartiQLValueType.SYMBOL -> StaticType.SYMBOL
            PartiQLValueType.BINARY -> null
            PartiQLValueType.BYTE -> null
            PartiQLValueType.BLOB -> StaticType.BLOB
            PartiQLValueType.CLOB -> StaticType.CLOB
            PartiQLValueType.DATE -> StaticType.DATE
            PartiQLValueType.TIME -> StaticType.TIME
            PartiQLValueType.TIMESTAMP -> StaticType.TIMESTAMP
            PartiQLValueType.INTERVAL -> null
            PartiQLValueType.BAG -> StaticType.BAG
            PartiQLValueType.LIST -> StaticType.LIST
            PartiQLValueType.SEXP -> StaticType.SEXP
            PartiQLValueType.STRUCT -> StaticType.STRUCT
            PartiQLValueType.NULL -> StaticType.NULL
            PartiQLValueType.MISSING -> StaticType.MISSING
        }

    fun testGen(
        targetStaticType: StaticType,
        targetPartiQLValueType: PartiQLValueType,
        testName: String,
        testId: Int
    ): Stream<DynamicContainer> {
        val tests = listOf(
            "cast-${testId.toString().padStart(2, '0')}",
        ).map { inputs.get("casts", it)!! }

        val argsMap = buildMap {
            val casts = PartiQLValueType.values().map { from ->
                CastTestsUtil.getCastKind(from, targetPartiQLValueType) to from
            }.groupBy(
                { it.first }, // cast result
                { it.second } // input type
            )

            (casts[null] ?: emptyList())
                .mapNotNull { inputs ->
                    inputs.valueTypeToStaticType()
                }
                .map { listOf(it) }
                .let {
                    put(TestResult.Failure, it.toSet())
                }

            casts
                .filterNot { it.key == null }
                .forEach { (castRes, inputs) ->
                    val i = inputs.mapNotNull { input ->
                        input.valueTypeToStaticType()
                    }.map { listOf(it) }.toSet()
                    put(TestResult.Success(castRes!!), i)
                }
        }

        return super.testGen(testName, tests, argsMap)
    }
}
