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

        // Does NOT handle MISSING
        val plus__INT_INT__INT__missing_call = fn("plus", PartiQLValueType.INT, PartiQLValueType.INT, PartiQLValueType.INT, isNullable = false)
        val plus__INT16_INT16__INT16_missing_call = fn("plus", PartiQLValueType.INT16, PartiQLValueType.INT16, PartiQLValueType.INT16, isNullable = false)

        // Handles MISSING
        val eq__INT16_INT16__BOOL__not_missing_call = fn("eq", PartiQLValueType.BOOL, PartiQLValueType.INT16, PartiQLValueType.INT16, isNullable = false, isMissingCall = false)
        val eq__ANY_ANY__BOOL__not_missing_call = fn("eq", PartiQLValueType.BOOL, PartiQLValueType.ANY, PartiQLValueType.ANY, isNullable = false, isMissingCall = false)

        // Specifically takes in MISSING and handles MISSING.
        val foo__MISSING__BOOL__not_missing_call = fn("foo", PartiQLValueType.BOOL, PartiQLValueType.MISSING, isNullable = false, isMissingCall = false)

        // Specifically takes in ANY/DYNAMIC and handles MISSING
        val bar__ANY__BOOL__not_missing_call = fn("bar", PartiQLValueType.BOOL, PartiQLValueType.ANY, isNullable = false, isMissingCall = false)

        // Specifically takes in ANY/DYNAMIC and does NOT handle MISSING
        val baz__ANY__BOOL__missing_call = fn("param_is_dynamic_not_missing", PartiQLValueType.BOOL, PartiQLValueType.ANY, isNullable = false, isMissingCall = true)

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
                plus__INT_INT__INT__missing_call,
                plus__INT16_INT16__INT16_missing_call,
                eq__INT16_INT16__BOOL__not_missing_call,
                eq__ANY_ANY__BOOL__not_missing_call,
                foo__MISSING__BOOL__not_missing_call,
                bar__ANY__BOOL__not_missing_call,
                baz__ANY__BOOL__missing_call
            )
        }

        private val resolver = FnResolver(myHeader)

        @JvmStatic
        fun allCases() = listOf(
            Case.ResolveScalarFn(
                name = "INT PLUS INT4",
                identifier = "plus",
                args = listOf(StaticType.INT, StaticType.INT4),
                expected = FnMatch.Ok(
                    signature = plus__INT_INT__INT__missing_call,
                    mapping = listOf(null, null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "INT2 PLUS INT2",
                identifier = "plus",
                args = listOf(StaticType.INT2, StaticType.INT2),
                expected = FnMatch.Ok(
                    signature = plus__INT16_INT16__INT16_missing_call,
                    mapping = listOf(null, null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "UNION(INT2|INT4) PLUS INT2",
                identifier = "plus",
                args = listOf(StaticType.unionOf(StaticType.INT2, StaticType.INT4), StaticType.INT2),
                expected = FnMatch.Dynamic(
                    candidates = listOf(
                        FnMatch.Ok(
                            signature = plus__INT16_INT16__INT16_missing_call,
                            mapping = listOf(null, null),
                            isMissable = false
                        ),
                        FnMatch.Ok(
                            signature = plus__INT_INT__INT__missing_call,
                            mapping = listOf(null, null),
                            isMissable = false
                        )
                    ),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "UNION(INT2|MISSING) PLUS INT2",
                identifier = "plus",
                args = listOf(StaticType.unionOf(StaticType.INT2, StaticType.MISSING), StaticType.INT2),
                expected = FnMatch.Ok(
                    signature = plus__INT16_INT16__INT16_missing_call,
                    mapping = listOf(null, null),
                    isMissable = true
                )
            ),
            Case.ResolveScalarFn(
                name = "UNION(INT2|INT4|MISSING) PLUS INT2",
                identifier = "plus",
                args = listOf(StaticType.unionOf(StaticType.INT2, StaticType.INT4, StaticType.MISSING), StaticType.INT2),
                expected = FnMatch.Dynamic(
                    candidates = listOf(
                        FnMatch.Ok(
                            signature = plus__INT16_INT16__INT16_missing_call,
                            mapping = listOf(null, null),
                            isMissable = false
                        ),
                        FnMatch.Ok(
                            signature = plus__INT_INT__INT__missing_call,
                            mapping = listOf(null, null),
                            isMissable = false
                        )
                    ),
                    isMissable = true
                )
            ),
            Case.ResolveScalarFn(
                name = "MISSING PLUS INT2",
                identifier = "plus",
                args = listOf(StaticType.MISSING, StaticType.INT2),
                expected = FnMatch.Error(
                    candidates = listOf(
                        plus__INT16_INT16__INT16_missing_call,
                        plus__INT_INT__INT__missing_call,
                    ),
                    identifier = Identifier.Symbol("plus", Identifier.CaseSensitivity.INSENSITIVE),
                    args = listOf(
                        Rex(StaticType.MISSING, Rex.Op.Var.Resolved(0)),
                        Rex(StaticType.INT2, Rex.Op.Var.Resolved(0)),
                    )
                )
            ),
            Case.ResolveScalarFn(
                name = "INT2 EQ INT2",
                identifier = "eq",
                args = listOf(StaticType.INT2, StaticType.INT2),
                expected = FnMatch.Ok(
                    signature = eq__INT16_INT16__BOOL__not_missing_call,
                    mapping = listOf(null, null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "DECIMAL EQ INT2",
                identifier = "eq",
                args = listOf(StaticType.DECIMAL, StaticType.INT2),
                expected = FnMatch.Ok(
                    signature = eq__ANY_ANY__BOOL__not_missing_call,
                    mapping = listOf(null, null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "UNION(DECIMAL|INT) EQ INT2",
                identifier = "eq",
                args = listOf(StaticType.unionOf(StaticType.DECIMAL, StaticType.INT), StaticType.INT2),
                expected = FnMatch.Ok(
                    signature = eq__ANY_ANY__BOOL__not_missing_call,
                    mapping = listOf(null, null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "BAR(INT) -- Parameter is DYNAMIC and handles MISSING. Pass in CONCRETE value.",
                identifier = bar__ANY__BOOL__not_missing_call.name,
                args = listOf(StaticType.INT),
                expected = FnMatch.Ok(
                    signature = bar__ANY__BOOL__not_missing_call,
                    mapping = listOf(null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "BAR(UNION(DECIMAL|INT)) -- Parameter is DYNAMIC and handles MISSING. Pass in UNION value.",
                identifier = bar__ANY__BOOL__not_missing_call.name,
                args = listOf(StaticType.unionOf(StaticType.DECIMAL, StaticType.INT)),
                expected = FnMatch.Ok(
                    signature = bar__ANY__BOOL__not_missing_call,
                    mapping = listOf(null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "BAR(UNION(DECIMAL|INT|MISSING)) -- Parameter is DYNAMIC and handles MISSING. Pass in UNION value with MISSING.",
                identifier = bar__ANY__BOOL__not_missing_call.name,
                args = listOf(StaticType.unionOf(StaticType.DECIMAL, StaticType.INT, StaticType.MISSING)),
                expected = FnMatch.Ok(
                    signature = bar__ANY__BOOL__not_missing_call,
                    mapping = listOf(null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "BAR(MISSING) -- Parameter is DYNAMIC and handles MISSING. Pass in MISSING value.",
                identifier = bar__ANY__BOOL__not_missing_call.name,
                args = listOf(StaticType.MISSING),
                expected = FnMatch.Ok(
                    signature = bar__ANY__BOOL__not_missing_call,
                    mapping = listOf(null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "BAZ(INT) -- Parameter is DYNAMIC but doesn't handle MISSING. Pass in CONCRETE value.",
                identifier = baz__ANY__BOOL__missing_call.name,
                args = listOf(StaticType.INT),
                expected = FnMatch.Ok(
                    signature = baz__ANY__BOOL__missing_call,
                    mapping = listOf(null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "BAZ(UNION(DECIMAL|INT)) -- Parameter is DYNAMIC but doesn't handle MISSING. Pass in UNION value.",
                identifier = baz__ANY__BOOL__missing_call.name,
                args = listOf(StaticType.unionOf(StaticType.DECIMAL, StaticType.INT)),
                expected = FnMatch.Ok(
                    signature = baz__ANY__BOOL__missing_call,
                    mapping = listOf(null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "BAZ(UNION(DECIMAL|INT|MISSING)) -- Parameter is DYNAMIC but doesn't handle MISSING. Pass in UNION value with MISSING.",
                identifier = baz__ANY__BOOL__missing_call.name,
                args = listOf(StaticType.unionOf(StaticType.DECIMAL, StaticType.INT, StaticType.MISSING)),
                expected = FnMatch.Ok(
                    signature = baz__ANY__BOOL__missing_call,
                    mapping = listOf(null),
                    isMissable = true
                )
            ),
            Case.ResolveScalarFn(
                name = "BAR(MISSING) -- Parameter is DYNAMIC but doesn't handle MISSING. Pass in MISSING value.",
                identifier = baz__ANY__BOOL__missing_call.name,
                args = listOf(StaticType.MISSING),
                expected = FnMatch.Ok(
                    signature = baz__ANY__BOOL__missing_call,
                    mapping = listOf(null),
                    isMissable = true
                )
            ),
            Case.ResolveScalarFn(
                name = "FOO(MISSING) -- Parameter is MISSING and handles MISSING. Pass in MISSING value.",
                identifier = foo__MISSING__BOOL__not_missing_call.name,
                args = listOf(StaticType.MISSING),
                expected = FnMatch.Ok(
                    signature = foo__MISSING__BOOL__not_missing_call,
                    mapping = listOf(null),
                    isMissable = false
                )
            ),
            Case.ResolveScalarFn(
                name = "FOO(UNION(INT|MISSING)) -- Parameter is MISSING and handles MISSING. Pass in UNION with MISSING.",
                identifier = foo__MISSING__BOOL__not_missing_call.name,
                args = listOf(StaticType.unionOf(StaticType.MISSING, StaticType.INT)),
                expected = FnMatch.Ok(
                    signature = foo__MISSING__BOOL__not_missing_call,
                    mapping = listOf(null),
                    isMissable = true
                )
            ),
            Case.ResolveScalarFn(
                name = "FOO(UNION(INT|MISSING)) -- Parameter is MISSING and handles MISSING. Pass in concrete value.",
                identifier = foo__MISSING__BOOL__not_missing_call.name,
                args = listOf(StaticType.INT),
                expected = FnMatch.Error(
                    identifier = Identifier.Symbol(foo__MISSING__BOOL__not_missing_call.name, Identifier.CaseSensitivity.INSENSITIVE),
                    args = listOf(Rex(StaticType.INT, Rex.Op.Var.Resolved(0))),
                    candidates = listOf(foo__MISSING__BOOL__not_missing_call),
                )
            ),
        )
    }

    sealed class Case {

        abstract fun assert()

        /**
         * A [Case] that specifically tests the dynamic/static functions that are resolved when passing in [args].
         *
         * NOTE: The equivalence of [expected] against what actually is returned does NOT take into consideration the
         * casts. This is due to difficulties in gathering the internal casts. This [Case] implementation is solely
         * focused on testing which functions are resolved.
         */
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
                val match = resolver.resolveFn(Fn.Unresolved(identifier, isHidden), rexArgs)
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
