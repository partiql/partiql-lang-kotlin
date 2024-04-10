package org.partiql.planner.internal.typer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.planner.internal.Header
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rex
import org.partiql.types.StaticType
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * As far as testing is concerned, we can stub out all value related things.
 * We may be able to pretty-print with string equals to also simplify things.
 * Only the "types" of expressions matter, we ignore the underlying ops.
 */
@OptIn(PartiQLValueExperimental::class)
internal class FunctionResolverTest {

    @ParameterizedTest
    @MethodSource("allCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun cases(tc: Case) {
        tc.assert()
    }

    @Test
    fun singleTest() {
        val tc =
            Case.ResolveScalarFn(
                name = "Higher Precedence PLUS",
                identifier = "plus",
                args = listOf(StaticType.INT2, StaticType.INT2),
                expected = FnMatch.Ok(
                    signature = plus2,
                    mapping = listOf(null, null),
                    isMissable = false
                )
            )
        tc.assert()
    }

    @Test
    fun sanity() {
        // 1 + 1.0 -> 2.0
        val fn = Header.binary(
            name = "plus",
            returns = PartiQLValueType.FLOAT64,
            lhs = PartiQLValueType.FLOAT64,
            rhs = PartiQLValueType.FLOAT64,
        )
        val args = listOf(
            FunctionParameter("arg-0", PartiQLValueType.INT32),
            FunctionParameter("arg-1", PartiQLValueType.FLOAT64),
        )
        val expectedImplicitCasts = listOf(true, false)
        val case = Case.Success(fn, args, expectedImplicitCasts)
        case.assert()
    }

    @Test
    fun split() {
        val args = listOf(
            FunctionParameter("arg-0", PartiQLValueType.STRING),
            FunctionParameter("arg-1", PartiQLValueType.STRING),
        )
        val expectedImplicitCasts = listOf(false, false)
        val case = Case.Success(split, args, expectedImplicitCasts)
        case.assert()
    }

    companion object {

        val split =
            fn("split", PartiQLValueType.LIST, PartiQLValueType.STRING, PartiQLValueType.STRING, isNullable = false)

        val plus1 = fn("plus", PartiQLValueType.INT, PartiQLValueType.INT, PartiQLValueType.INT, isNullable = false)
        val plus2 = fn("plus", PartiQLValueType.INT8, PartiQLValueType.INT16, PartiQLValueType.INT16, isNullable = false)

        // Handles MISSING
        val eq1 = fn("eq", PartiQLValueType.BOOL, PartiQLValueType.INT16, PartiQLValueType.INT16, isNullable = false, isMissingCall = false)
        val eq2 = fn("eq", PartiQLValueType.BOOL, PartiQLValueType.ANY, PartiQLValueType.ANY, isNullable = false, isMissingCall = false)

        private fun fn(
            name: String,
            returns: PartiQLValueType,
            vararg args: PartiQLValueType,
            isNullable: Boolean = true,
            isNullCall: Boolean = false,
            isMissable: Boolean = false,
            isMissingCall: Boolean = true
        ) = FunctionSignature.Scalar(
            name = name,
            returns = returns,
            parameters = args.mapIndexed { index, paramType -> FunctionParameter("p$index", paramType) },
            isNullable = isNullable,
            isNullCall = isNullCall,
            isMissable = isMissable,
            isMissingCall = isMissingCall
        )

        private val myHeader = object : Header() {

            override val namespace: String = "my_header"

            override val functions: List<FunctionSignature.Scalar> = listOf(
                split,
                plus1,
                plus2,
                eq1,
                eq2
            )
        }

        private val resolver = FnResolver(myHeader)

        @JvmStatic
        fun allCases() = listOf(
            Case.ResolveScalarFn(
                name = "Implicit Coercion PLUS",
                identifier = "plus",
                args = listOf(StaticType.INT, StaticType.INT4),
                expected = FnMatch.Ok(
                    signature = plus1,
                    mapping = listOf(null, null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "Higher Precedence PLUS",
                identifier = "plus",
                args = listOf(StaticType.INT2, StaticType.INT2),
                expected = FnMatch.Ok(
                    signature = plus2,
                    mapping = listOf(null, null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "UNION PLUS",
                identifier = "plus",
                args = listOf(StaticType.unionOf(StaticType.INT2, StaticType.MISSING), StaticType.INT2),
                expected = FnMatch.Ok(
                    signature = plus2,
                    mapping = listOf(null, null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "Straightforward EQ",
                identifier = "eq",
                args = listOf(StaticType.INT2, StaticType.INT2),
                expected = FnMatch.Ok(
                    signature = eq1,
                    mapping = listOf(null, null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "ANY EQ",
                identifier = "eq",
                args = listOf(StaticType.DECIMAL, StaticType.INT8),
                expected = FnMatch.Ok(
                    signature = eq2,
                    mapping = listOf(null, null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "UNION EQ",
                identifier = "eq",
                args = listOf(StaticType.unionOf(StaticType.DECIMAL, StaticType.INT), StaticType.INT8),
                expected = FnMatch.Ok(
                    signature = eq2,
                    mapping = listOf(null, null),
                    isMissable = false
                )
            ),
        )
    }

    sealed class Case {

        abstract fun assert()

        class ResolveScalarFn(
            private val name: String,
            private val identifier: String,
            private val isHidden: Boolean = false,
            private val args: List<StaticType>,
            private val expected: FnMatch<FunctionSignature.Scalar>
        ) : Case() {

            override fun assert() {
                val identifier = Identifier.Symbol(identifier, Identifier.CaseSensitivity.INSENSITIVE)
                val rexArgs = args.map { Rex(it, Rex.Op.Var.Resolved(0)) }
                val match = resolver.resolveFnScalar(Fn.Unresolved(identifier, isHidden), rexArgs)
                assert(matches(expected, match)) {
                    errorMessage(expected, match)
                }
            }

            private fun matches(expected: FnMatch<FunctionSignature.Scalar>, actual: FnMatch<FunctionSignature.Scalar>): Boolean {
                return when {
                    expected is FnMatch.Ok && actual is FnMatch.Ok -> matches(expected, actual)
                    expected is FnMatch.Dynamic && actual is FnMatch.Dynamic -> matches(expected, actual)
                    expected is FnMatch.Error && actual is FnMatch.Error -> expected == actual
                    else -> false
                }
            }

            /**
             * Ignores the casts for testing.
             */
            private fun matches(expected: FnMatch.Ok<FunctionSignature.Scalar>, actual: FnMatch.Ok<FunctionSignature.Scalar>): Boolean {
                return (expected.signature == actual.signature) && (expected.isMissable == actual.isMissable)
            }

            private fun matches(expected: FnMatch.Dynamic<FunctionSignature.Scalar>, actual: FnMatch.Dynamic<FunctionSignature.Scalar>): Boolean {
                if (expected.candidates.size != actual.candidates.size) {
                    return false
                }
                expected.candidates.forEachIndexed { index, ok ->
                    if (!matches(ok, actual.candidates[index])) {
                        return false
                    }
                }
                return expected.isMissable == actual.isMissable
            }

            private fun errorMessage(expected: FnMatch<FunctionSignature.Scalar>, actual: FnMatch<FunctionSignature.Scalar>): String {
                return buildString {
                    appendLine("Expected  : $expected")
                    appendLine("Actual    : $actual")
                }
            }

            override fun toString(): String = this.name
        }

        class Success(
            private val signature: FunctionSignature,
            private val inputs: List<FunctionParameter>,
            private val expectedImplicitCast: List<Boolean>,
        ) : Case() {

            /**
             * Assert we match the function, and the appropriate implicit CASTs were returned.
             *
             * TODO actually look into what the CAST functions are.
             */
            override fun assert() {
                val mapping = resolver.match(signature, inputs)
                val diffs = mutableListOf<String>()
                val message = buildString {
                    appendLine("Given arguments did not match function signature")
                    appendLine(signature)
                    appendLine("Input: (${inputs.joinToString()}})")
                }
                if (mapping == null || mapping.size != expectedImplicitCast.size) {
                    fail { message }
                }
                // compare args
                for (i in mapping.indices) {
                    val m = mapping[i]
                    val shouldCast = expectedImplicitCast[i]
                    val diff: String? = when {
                        m == null && shouldCast -> "Arg[$i] is missing an implicit CAST"
                        m != null && !shouldCast -> "Arg[$i] had implicit CAST but should not"
                        else -> null
                    }
                    if (diff != null) diffs.add(diff)
                }
                // pretty-print some debug info
                if (diffs.isNotEmpty()) {
                    fail {
                        buildString {
                            appendLine(message)
                            diffs.forEach { appendLine(it) }
                        }
                    }
                }
            }
        }
    }
}
