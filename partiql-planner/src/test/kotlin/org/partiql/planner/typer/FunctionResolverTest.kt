package org.partiql.planner.typer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.partiql.plan.Fn
import org.partiql.plan.Plan
import org.partiql.plan.Rex
import org.partiql.planner.Header
import org.partiql.types.PartiQLValueType
import org.partiql.types.StaticType
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.float64Value
import org.partiql.value.int32Value

/**
 * As far as testing is concerned, we can stub out all value related things.
 * We may be able to pretty-print with string equals to also simplify things.
 * Only the "types" of expressions matter, we ignore the underlying ops.
 */
@OptIn(PartiQLValueExperimental::class)
class FunctionResolverTest {

    @Test
    fun sanity() {

        // TEST
        // 1 + 1.0 -> 2.0

        // Input arguments
        // [1, 1.0]

        val args = mutableListOf<Rex.Op.Call.Arg>()
        with(Plan) {
            args.add(rexOpCallArgValue(rex(StaticType.INT4, rexOpLit(int32Value(1)))))
            args.add(rexOpCallArgValue(rex(StaticType.FLOAT, rexOpLit(float64Value(1.0)))))
        }

        // Matched arguments
        // [CAST(1 AS FLOAT), 1.0]

        val expected = mutableListOf<Rex.Op.Call.Arg>()
        val implicitCast = Header.Functions.cast(PartiQLValueType.INT32, PartiQLValueType.FLOAT64)
        expected.add(FunctionResolver.castArg(args[0] as Rex.Op.Call.Arg.Value, implicitCast))
        expected.add(args[1])

        // Construct the function we want to match against
        val case = Case.Success(
            signature = Header.Functions.binary(
                name = "plus",
                returns = PartiQLValueType.FLOAT64,
                lhs = PartiQLValueType.FLOAT64,
                rhs = PartiQLValueType.FLOAT64,
            ),
            inputs = args,
            expected = expected,
        )
        case.assert()
    }

    companion object {

        private val header = Header.partiql()
        private val resolver = FunctionResolver(header)

        // create a CAST function; could make the header one internally visible
        private fun cast(t1: PartiQLValueType, t2: PartiQLValueType) = FunctionSignature(
            name = "cast",
            returns = t2,
            parameters = listOf(
                FunctionParameter.V("value", t1),
                FunctionParameter.T("type", t2),
            )
        )
    }

    private sealed class Case {

        abstract fun assert()

        class Success(
            private val signature: FunctionSignature,
            private val inputs: Args,
            private val expected: Args,
        ) : Case() {

            override fun assert() {
                val actual = resolver.match(signature, inputs)
                val diffs = mutableListOf<String>()
                val message = buildString {
                    appendLine("Given arguments did not match function signature")
                    appendLine(signature)
                    appendLine("Args: ${inputs.joinToString { it.str() }}")
                }
                if (actual == null || actual.size != expected.size) {
                    fail { message }
                }
                // compare args
                for (i in expected.indices) {
                    val e = expected[i]
                    val a = actual[i]
                    val diff: String? = when {
                        (e === a) -> null
                        (e is Rex.Op.Call.Arg.Type && a is Rex.Op.Call.Arg.Value) -> "Arg[$i] is a value, expected a type"
                        (e is Rex.Op.Call.Arg.Value && a is Rex.Op.Call.Arg.Type) -> "Arg[$i] is a type, expected a value"
                        (e is Rex.Op.Call.Arg.Value && a is Rex.Op.Call.Arg.Value) -> diff(i, e, a)
                        (e is Rex.Op.Call.Arg.Type && a is Rex.Op.Call.Arg.Type) -> diff(i, e, a)
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

        // returns a diff message iff difference, returns null if types are the same
        internal fun diff(index: Int, expected: Rex.Op.Call.Arg.Type, actual: Rex.Op.Call.Arg.Type): String? {
            if (expected === actual) return null
            if (expected.type != actual.type) {
                return "Arg[$index] was $actual, expected $expected"
            }
            return null
        }

        // returns a diff iff different, returns null if expressions are the same
        internal fun diff(index: Int, expected: Rex.Op.Call.Arg.Value, actual: Rex.Op.Call.Arg.Value): String? {
            if (expected === actual) return null
            // If references were different, then an implicit cast was inserted
            val eOp = expected.rex.op
            val aOp = actual.rex.op
            // This is bad code
            if (eOp !is Rex.Op.Call || eOp.fn !is Fn.Resolved) {
                return "Arg[$index] should be an implicit CAST"
            }
            if (aOp !is Rex.Op.Call || aOp.fn !is Fn.Resolved) {
                return "Arg[$index] did not match, expected an implicit CAST function"
            }
            val fnExpected = (eOp.fn as Fn.Resolved).signature
            val fnActual = (eOp.fn as Fn.Resolved).signature
            if (fnExpected != fnActual) {
                return "Arg[$index] expected to be implicit CAST to ${fnExpected.returns}, found CAST to ${fnActual.returns}"
            }
            return null
        }

        internal fun Rex.Op.Call.Arg.str(): String = when (this) {
            is Rex.Op.Call.Arg.Type -> "T($type)"
            is Rex.Op.Call.Arg.Value -> "V(${rex.type})"
        }
    }
}
