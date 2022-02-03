package org.partiql.lang.ast.passes.inference

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.partiql.lang.types.DecimalType
import org.partiql.lang.types.IntType
import org.partiql.lang.types.StaticType

@RunWith(JUnitParamsRunner::class)
class StaticTypeCastTests {

    data class TestCase(
        val sourceType: StaticType,
        val targetType: StaticType,
        val expectedType: StaticType
    )

    private fun runTest(tc: TestCase) {
        val outType = tc.sourceType.cast(tc.targetType)
        assertEquals("Expected ${tc.expectedType} when ${tc.sourceType} is casted to ${tc.targetType}", tc.expectedType, outType)
    }

    @Test
    @Parameters
    fun nullCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun missingCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun boolCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun intCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun floatCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun decimalCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun timestampCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun symbolCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun stringCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun clobCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun blobCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun listCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun bagCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun sexpCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun structCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun unionTypeCastTests(tc: TestCase) = runTest(tc)

    @Test
    @Parameters
    fun numberCastTests(tc: TestCase) = runTest(tc)

    companion object {
        private val numberType = StaticType.unionOf(StaticType.INT, StaticType.FLOAT, StaticType.DECIMAL)
        private val numberOrMissingType = StaticType.unionOf(StaticType.MISSING, numberType)
        private val numberOrUnknownType = StaticType.unionOf(StaticType.MISSING, StaticType.NULL, numberType)

        fun List<TestCase>.addCastToAnyCases(): List<TestCase> = this + this.map{
            it.copy(
                targetType = StaticType.ANY,
                expectedType = it.sourceType
            )
        }
    }

    fun parametersForNullCastTests() = listOf(
        TestCase(StaticType.NULL, StaticType.NULL, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.NULL, StaticType.BOOL, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.INT, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.FLOAT, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.DECIMAL, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.TIMESTAMP, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.SYMBOL, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.STRING, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.CLOB, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.BLOB, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.LIST, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.SEXP, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.BAG, StaticType.NULL),
        TestCase(StaticType.NULL, StaticType.STRUCT, StaticType.NULL),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.NULL, numberType, numberOrUnknownType)
    ).addCastToAnyCases()

    fun parametersForMissingCastTests() = listOf(
        TestCase(StaticType.MISSING, StaticType.NULL, StaticType.NULL),
        TestCase(StaticType.MISSING, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.BOOL, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.INT, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.FLOAT, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.DECIMAL, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.TIMESTAMP, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.SYMBOL, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.STRING, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.CLOB, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.BLOB, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.LIST, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.SEXP, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.BAG, StaticType.MISSING),
        TestCase(StaticType.MISSING, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.MISSING, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForBoolCastTests() = listOf(
        TestCase(StaticType.BOOL, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.BOOL, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.BOOL, StaticType.BOOL, StaticType.BOOL),
        TestCase(StaticType.BOOL, StaticType.INT, StaticType.INT),
        TestCase(StaticType.BOOL, StaticType.FLOAT, StaticType.FLOAT),
        TestCase(StaticType.BOOL, StaticType.DECIMAL, StaticType.DECIMAL),
        TestCase(StaticType.BOOL, StaticType.TIMESTAMP, StaticType.MISSING),
        TestCase(StaticType.BOOL, StaticType.SYMBOL, StaticType.SYMBOL),
        TestCase(StaticType.BOOL, StaticType.STRING, StaticType.STRING),
        TestCase(StaticType.BOOL, StaticType.CLOB, StaticType.MISSING),
        TestCase(StaticType.BOOL, StaticType.BLOB, StaticType.MISSING),
        TestCase(StaticType.BOOL, StaticType.LIST, StaticType.MISSING),
        TestCase(StaticType.BOOL, StaticType.SEXP, StaticType.MISSING),
        TestCase(StaticType.BOOL, StaticType.BAG, StaticType.MISSING),
        TestCase(StaticType.BOOL, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.BOOL, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForIntCastTests() = listOf(
        TestCase(StaticType.INT, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.INT, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.INT, StaticType.BOOL, StaticType.BOOL),
        TestCase(StaticType.INT, StaticType.INT, StaticType.INT),
        TestCase(StaticType.INT, StaticType.FLOAT, StaticType.FLOAT),
        TestCase(StaticType.INT, StaticType.DECIMAL, StaticType.DECIMAL),
        TestCase(StaticType.INT, StaticType.TIMESTAMP, StaticType.MISSING),
        TestCase(StaticType.INT, StaticType.SYMBOL, StaticType.SYMBOL),
        TestCase(StaticType.INT, StaticType.STRING, StaticType.STRING),
        TestCase(StaticType.INT, StaticType.CLOB, StaticType.MISSING),
        TestCase(StaticType.INT, StaticType.BLOB, StaticType.MISSING),
        TestCase(StaticType.INT, StaticType.LIST, StaticType.MISSING),
        TestCase(StaticType.INT, StaticType.SEXP, StaticType.MISSING),
        TestCase(StaticType.INT, StaticType.BAG, StaticType.MISSING),
        TestCase(StaticType.INT, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.INT, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForFloatCastTests() = listOf(
        TestCase(StaticType.FLOAT, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.FLOAT, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.FLOAT, StaticType.BOOL, StaticType.BOOL),
        TestCase(StaticType.FLOAT, StaticType.INT, StaticType.INT),
        TestCase(StaticType.FLOAT, StaticType.FLOAT, StaticType.FLOAT),
        TestCase(StaticType.FLOAT, StaticType.DECIMAL, StaticType.DECIMAL),
        TestCase(StaticType.FLOAT, StaticType.TIMESTAMP, StaticType.MISSING),
        TestCase(StaticType.FLOAT, StaticType.SYMBOL, StaticType.SYMBOL),
        TestCase(StaticType.FLOAT, StaticType.STRING, StaticType.STRING),
        TestCase(StaticType.FLOAT, StaticType.CLOB, StaticType.MISSING),
        TestCase(StaticType.FLOAT, StaticType.BLOB, StaticType.MISSING),
        TestCase(StaticType.FLOAT, StaticType.LIST, StaticType.MISSING),
        TestCase(StaticType.FLOAT, StaticType.SEXP, StaticType.MISSING),
        TestCase(StaticType.FLOAT, StaticType.BAG, StaticType.MISSING),
        TestCase(StaticType.FLOAT, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.FLOAT, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForDecimalCastTests() = listOf(
        TestCase(StaticType.DECIMAL, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.DECIMAL, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.DECIMAL, StaticType.BOOL, StaticType.BOOL),
        TestCase(StaticType.DECIMAL, StaticType.INT, StaticType.INT),
        TestCase(StaticType.DECIMAL, StaticType.FLOAT, StaticType.FLOAT),
        TestCase(StaticType.DECIMAL, StaticType.DECIMAL, StaticType.DECIMAL),
        TestCase(StaticType.DECIMAL, StaticType.TIMESTAMP, StaticType.MISSING),
        TestCase(StaticType.DECIMAL, StaticType.SYMBOL, StaticType.SYMBOL),
        TestCase(StaticType.DECIMAL, StaticType.STRING, StaticType.STRING),
        TestCase(StaticType.DECIMAL, StaticType.CLOB, StaticType.MISSING),
        TestCase(StaticType.DECIMAL, StaticType.BLOB, StaticType.MISSING),
        TestCase(StaticType.DECIMAL, StaticType.LIST, StaticType.MISSING),
        TestCase(StaticType.DECIMAL, StaticType.SEXP, StaticType.MISSING),
        TestCase(StaticType.DECIMAL, StaticType.BAG, StaticType.MISSING),
        TestCase(StaticType.DECIMAL, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.DECIMAL, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForTimestampCastTests() = listOf(
        TestCase(StaticType.TIMESTAMP, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.TIMESTAMP, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.TIMESTAMP, StaticType.BOOL, StaticType.MISSING),
        TestCase(StaticType.TIMESTAMP, StaticType.INT, StaticType.MISSING),
        TestCase(StaticType.TIMESTAMP, StaticType.FLOAT, StaticType.MISSING),
        TestCase(StaticType.TIMESTAMP, StaticType.DECIMAL, StaticType.MISSING),
        TestCase(StaticType.TIMESTAMP, StaticType.TIMESTAMP, StaticType.TIMESTAMP),
        TestCase(StaticType.TIMESTAMP, StaticType.SYMBOL, StaticType.SYMBOL),
        TestCase(StaticType.TIMESTAMP, StaticType.STRING, StaticType.STRING),
        TestCase(StaticType.TIMESTAMP, StaticType.CLOB, StaticType.MISSING),
        TestCase(StaticType.TIMESTAMP, StaticType.BLOB, StaticType.MISSING),
        TestCase(StaticType.TIMESTAMP, StaticType.LIST, StaticType.MISSING),
        TestCase(StaticType.TIMESTAMP, StaticType.SEXP, StaticType.MISSING),
        TestCase(StaticType.TIMESTAMP, StaticType.BAG, StaticType.MISSING),
        TestCase(StaticType.TIMESTAMP, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.TIMESTAMP, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForSymbolCastTests() = listOf(
        TestCase(StaticType.SYMBOL, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.SYMBOL, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.SYMBOL, StaticType.BOOL, StaticType.BOOL),
        TestCase(StaticType.SYMBOL, StaticType.INT, StaticType.unionOf(StaticType.INT, StaticType.MISSING)),
        TestCase(StaticType.SYMBOL, StaticType.FLOAT, StaticType.unionOf(StaticType.FLOAT, StaticType.MISSING)),
        TestCase(StaticType.SYMBOL, StaticType.DECIMAL, StaticType.unionOf(StaticType.DECIMAL, StaticType.MISSING)),
        TestCase(StaticType.SYMBOL, StaticType.TIMESTAMP, StaticType.unionOf(StaticType.TIMESTAMP, StaticType.MISSING)),
        TestCase(StaticType.SYMBOL, StaticType.SYMBOL, StaticType.SYMBOL),
        TestCase(StaticType.SYMBOL, StaticType.STRING, StaticType.STRING),
        TestCase(StaticType.SYMBOL, StaticType.CLOB, StaticType.MISSING),
        TestCase(StaticType.SYMBOL, StaticType.BLOB, StaticType.MISSING),
        TestCase(StaticType.SYMBOL, StaticType.LIST, StaticType.MISSING),
        TestCase(StaticType.SYMBOL, StaticType.SEXP, StaticType.MISSING),
        TestCase(StaticType.SYMBOL, StaticType.BAG, StaticType.MISSING),
        TestCase(StaticType.SYMBOL, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.SYMBOL, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForStringCastTests() = listOf(
        TestCase(StaticType.STRING, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.STRING, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.STRING, StaticType.BOOL, StaticType.BOOL),
        TestCase(StaticType.STRING, StaticType.INT, StaticType.unionOf(StaticType.INT, StaticType.MISSING)),
        TestCase(StaticType.STRING, StaticType.FLOAT, StaticType.unionOf(StaticType.FLOAT, StaticType.MISSING)),
        TestCase(StaticType.STRING, StaticType.DECIMAL, StaticType.unionOf(StaticType.DECIMAL, StaticType.MISSING)),
        TestCase(StaticType.STRING, StaticType.TIMESTAMP, StaticType.unionOf(StaticType.TIMESTAMP, StaticType.MISSING)),
        TestCase(StaticType.STRING, StaticType.SYMBOL, StaticType.SYMBOL),
        TestCase(StaticType.STRING, StaticType.STRING, StaticType.STRING),
        TestCase(StaticType.STRING, StaticType.CLOB, StaticType.MISSING),
        TestCase(StaticType.STRING, StaticType.BLOB, StaticType.MISSING),
        TestCase(StaticType.STRING, StaticType.LIST, StaticType.MISSING),
        TestCase(StaticType.STRING, StaticType.SEXP, StaticType.MISSING),
        TestCase(StaticType.STRING, StaticType.BAG, StaticType.MISSING),
        TestCase(StaticType.STRING, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.STRING, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForClobCastTests() = listOf(
        TestCase(StaticType.CLOB, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.CLOB, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.CLOB, StaticType.BOOL, StaticType.MISSING),
        TestCase(StaticType.CLOB, StaticType.INT, StaticType.MISSING),
        TestCase(StaticType.CLOB, StaticType.FLOAT, StaticType.MISSING),
        TestCase(StaticType.CLOB, StaticType.DECIMAL, StaticType.MISSING),
        TestCase(StaticType.CLOB, StaticType.TIMESTAMP, StaticType.MISSING),
        TestCase(StaticType.CLOB, StaticType.SYMBOL, StaticType.MISSING),
        TestCase(StaticType.CLOB, StaticType.STRING, StaticType.MISSING),
        TestCase(StaticType.CLOB, StaticType.CLOB, StaticType.CLOB),
        TestCase(StaticType.CLOB, StaticType.BLOB, StaticType.BLOB),
        TestCase(StaticType.CLOB, StaticType.LIST, StaticType.MISSING),
        TestCase(StaticType.CLOB, StaticType.SEXP, StaticType.MISSING),
        TestCase(StaticType.CLOB, StaticType.BAG, StaticType.MISSING),
        TestCase(StaticType.CLOB, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.CLOB, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForBlobCastTests() = listOf(
        TestCase(StaticType.BLOB, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.BLOB, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.BLOB, StaticType.BOOL, StaticType.MISSING),
        TestCase(StaticType.BLOB, StaticType.INT, StaticType.MISSING),
        TestCase(StaticType.BLOB, StaticType.FLOAT, StaticType.MISSING),
        TestCase(StaticType.BLOB, StaticType.DECIMAL, StaticType.MISSING),
        TestCase(StaticType.BLOB, StaticType.TIMESTAMP, StaticType.MISSING),
        TestCase(StaticType.BLOB, StaticType.SYMBOL, StaticType.MISSING),
        TestCase(StaticType.BLOB, StaticType.STRING, StaticType.MISSING),
        TestCase(StaticType.BLOB, StaticType.CLOB, StaticType.CLOB),
        TestCase(StaticType.BLOB, StaticType.BLOB, StaticType.BLOB),
        TestCase(StaticType.BLOB, StaticType.LIST, StaticType.MISSING),
        TestCase(StaticType.BLOB, StaticType.SEXP, StaticType.MISSING),
        TestCase(StaticType.BLOB, StaticType.BAG, StaticType.MISSING),
        TestCase(StaticType.BLOB, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.BLOB, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForListCastTests() = listOf(
        TestCase(StaticType.LIST, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.LIST, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.LIST, StaticType.BOOL, StaticType.MISSING),
        TestCase(StaticType.LIST, StaticType.INT, StaticType.MISSING),
        TestCase(StaticType.LIST, StaticType.FLOAT, StaticType.MISSING),
        TestCase(StaticType.LIST, StaticType.DECIMAL, StaticType.MISSING),
        TestCase(StaticType.LIST, StaticType.TIMESTAMP, StaticType.MISSING),
        TestCase(StaticType.LIST, StaticType.SYMBOL, StaticType.MISSING),
        TestCase(StaticType.LIST, StaticType.STRING, StaticType.MISSING),
        TestCase(StaticType.LIST, StaticType.CLOB, StaticType.MISSING),
        TestCase(StaticType.LIST, StaticType.BLOB, StaticType.MISSING),
        TestCase(StaticType.LIST, StaticType.LIST, StaticType.LIST),
        TestCase(StaticType.LIST, StaticType.SEXP, StaticType.SEXP),
        TestCase(StaticType.LIST, StaticType.BAG, StaticType.BAG),
        TestCase(StaticType.LIST, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.LIST, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForSexpCastTests() = listOf(
        TestCase(StaticType.SEXP, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.SEXP, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.SEXP, StaticType.BOOL, StaticType.MISSING),
        TestCase(StaticType.SEXP, StaticType.INT, StaticType.MISSING),
        TestCase(StaticType.SEXP, StaticType.FLOAT, StaticType.MISSING),
        TestCase(StaticType.SEXP, StaticType.DECIMAL, StaticType.MISSING),
        TestCase(StaticType.SEXP, StaticType.TIMESTAMP, StaticType.MISSING),
        TestCase(StaticType.SEXP, StaticType.SYMBOL, StaticType.MISSING),
        TestCase(StaticType.SEXP, StaticType.STRING, StaticType.MISSING),
        TestCase(StaticType.SEXP, StaticType.CLOB, StaticType.MISSING),
        TestCase(StaticType.SEXP, StaticType.BLOB, StaticType.MISSING),
        TestCase(StaticType.SEXP, StaticType.LIST, StaticType.LIST),
        TestCase(StaticType.SEXP, StaticType.SEXP, StaticType.SEXP),
        TestCase(StaticType.SEXP, StaticType.BAG, StaticType.BAG),
        TestCase(StaticType.SEXP, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.SEXP, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForBagCastTests() = listOf(
        TestCase(StaticType.BAG, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.BAG, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.BAG, StaticType.BOOL, StaticType.MISSING),
        TestCase(StaticType.BAG, StaticType.INT, StaticType.MISSING),
        TestCase(StaticType.BAG, StaticType.FLOAT, StaticType.MISSING),
        TestCase(StaticType.BAG, StaticType.DECIMAL, StaticType.MISSING),
        TestCase(StaticType.BAG, StaticType.TIMESTAMP, StaticType.MISSING),
        TestCase(StaticType.BAG, StaticType.SYMBOL, StaticType.MISSING),
        TestCase(StaticType.BAG, StaticType.STRING, StaticType.MISSING),
        TestCase(StaticType.BAG, StaticType.CLOB, StaticType.MISSING),
        TestCase(StaticType.BAG, StaticType.BLOB, StaticType.MISSING),
        TestCase(StaticType.BAG, StaticType.LIST, StaticType.LIST),
        TestCase(StaticType.BAG, StaticType.SEXP, StaticType.SEXP),
        TestCase(StaticType.BAG, StaticType.BAG, StaticType.BAG),
        TestCase(StaticType.BAG, StaticType.STRUCT, StaticType.MISSING),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.BAG, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForStructCastTests() = listOf(
        TestCase(StaticType.STRUCT, StaticType.NULL, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.BOOL, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.INT, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.FLOAT, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.DECIMAL, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.TIMESTAMP, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.SYMBOL, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.STRING, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.CLOB, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.BLOB, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.LIST, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.SEXP, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.BAG, StaticType.MISSING),
        TestCase(StaticType.STRUCT, StaticType.STRUCT, StaticType.STRUCT),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.STRUCT, numberType, numberOrMissingType)
    ).addCastToAnyCases()

    fun parametersForUnionTypeCastTests() = listOf(
        // ANY includes NULL. casting from NULL to NULL returns NULL, rest of them return MISSING
        TestCase(StaticType.ANY, StaticType.NULL, StaticType.unionOf(StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.MISSING, StaticType.MISSING),
        TestCase(StaticType.ANY, StaticType.BOOL, StaticType.unionOf(StaticType.BOOL, StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.INT, StaticType.unionOf(StaticType.INT, StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.FLOAT, StaticType.unionOf(StaticType.FLOAT, StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.DECIMAL, StaticType.unionOf(StaticType.DECIMAL, StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.TIMESTAMP, StaticType.unionOf(StaticType.TIMESTAMP, StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.SYMBOL, StaticType.unionOf(StaticType.SYMBOL, StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.STRING, StaticType.unionOf(StaticType.STRING, StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.CLOB, StaticType.unionOf(StaticType.CLOB, StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.BLOB, StaticType.unionOf(StaticType.BLOB, StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.LIST, StaticType.unionOf(StaticType.LIST, StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.SEXP, StaticType.unionOf(StaticType.SEXP, StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.BAG, StaticType.unionOf(StaticType.BAG, StaticType.NULL, StaticType.MISSING)),
        TestCase(StaticType.ANY, StaticType.STRUCT, StaticType.unionOf(StaticType.STRUCT, StaticType.NULL, StaticType.MISSING)),
        // TODO make AnyOf type casting more specific
        TestCase(StaticType.ANY, numberType, numberOrUnknownType),

        // a union type that is not ANY
        TestCase(StaticType.unionOf(StaticType.INT, StaticType.FLOAT, StaticType.DECIMAL), StaticType.STRING, StaticType.STRING)
    ).addCastToAnyCases()

    fun parametersForNumberCastTests(): List<TestCase> {
        val smallint = IntType(IntType.IntRangeConstraint.SHORT)
        val int4 = IntType(IntType.IntRangeConstraint.INT4)
        val bigint = IntType(IntType.IntRangeConstraint.LONG)
        val unconstrainedInt = IntType(IntType.IntRangeConstraint.UNCONSTRAINED)

        val decimal4_2 = DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(4, 2))
        val decimal7_2 = DecimalType( DecimalType.PrecisionScaleConstraint.Constrained(7, 2))
        val decimal32_0 = DecimalType( DecimalType.PrecisionScaleConstraint.Constrained(32, 0))

        return listOf(
            TestCase(smallint, smallint, smallint),
            TestCase(smallint, int4, int4),
            TestCase(smallint, bigint, bigint),
            TestCase(smallint, unconstrainedInt, unconstrainedInt),

            TestCase(int4, smallint, StaticType.unionOf(StaticType.MISSING, smallint)),
            TestCase(int4, int4, int4),
            TestCase(int4, bigint, bigint),
            TestCase(int4, unconstrainedInt, unconstrainedInt),

            TestCase(bigint, smallint, StaticType.unionOf(StaticType.MISSING, smallint)),
            TestCase(bigint, int4, StaticType.unionOf(StaticType.MISSING, int4)),
            TestCase(bigint, bigint, bigint),
            TestCase(bigint, unconstrainedInt, unconstrainedInt),

            TestCase(unconstrainedInt, smallint, StaticType.unionOf(StaticType.MISSING, smallint)),
            TestCase(unconstrainedInt, int4, StaticType.unionOf(StaticType.MISSING, int4)),
            TestCase(unconstrainedInt, bigint, StaticType.unionOf(StaticType.MISSING, bigint)),
            TestCase(unconstrainedInt, unconstrainedInt, unconstrainedInt),

            TestCase(StaticType.DECIMAL, decimal4_2, StaticType.unionOf(StaticType.MISSING, decimal4_2)),
            TestCase(decimal4_2, StaticType.DECIMAL, StaticType.DECIMAL),
            TestCase(decimal4_2, decimal7_2, decimal7_2),
            TestCase(decimal7_2, decimal4_2, StaticType.unionOf(StaticType.MISSING, decimal4_2)),

            TestCase(smallint, decimal4_2, StaticType.unionOf(StaticType.MISSING, decimal4_2)),
            TestCase(int4, decimal4_2, StaticType.unionOf(StaticType.MISSING, decimal4_2)),
            TestCase(bigint, decimal4_2, StaticType.unionOf(StaticType.MISSING, decimal4_2)),
            TestCase(unconstrainedInt, decimal4_2, StaticType.unionOf(StaticType.MISSING, decimal4_2)),

            TestCase(decimal32_0, smallint, StaticType.unionOf(StaticType.MISSING, smallint)),
            TestCase(decimal32_0, int4, StaticType.unionOf(StaticType.MISSING, int4)),
            TestCase(decimal32_0, bigint, StaticType.unionOf(StaticType.MISSING, bigint)),
            TestCase(decimal32_0, unconstrainedInt, unconstrainedInt),


            TestCase(StaticType.FLOAT, smallint, StaticType.unionOf(StaticType.MISSING, smallint)),
            TestCase(decimal4_2, smallint, smallint),

            TestCase(smallint, decimal7_2, decimal7_2)
        ).addCastToAnyCases()
    }
}