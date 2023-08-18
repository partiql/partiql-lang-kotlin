package org.partiql.planner.typer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.partiql.planner.Header
import org.partiql.types.PartiQLValueType
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature

/**
 * As far as testing is concerned, we can stub out all value related things.
 * We may be able to pretty-print with string equals to also simplify things.
 * Only the "types" of expressions matter, we ignore the underlying ops.
 */
class FunctionResolverTest {

    @Test
    fun sanity() {
        // 1 + 1.0 -> 2.0
        val fn = Header.Functions.binary(
            name = "plus",
            returns = PartiQLValueType.FLOAT64,
            lhs = PartiQLValueType.FLOAT64,
            rhs = PartiQLValueType.FLOAT64,
        )
        val args = listOf(
            FunctionParameter.V("0", PartiQLValueType.INT32),
            FunctionParameter.V("1", PartiQLValueType.FLOAT64),
        )
        val expectedImplicitCasts = listOf(true, false)
        val case = Case.Success(fn, args, expectedImplicitCasts)
        case.assert()
    }

    companion object {
        private val header = Header.partiql()
        private val resolver = FunctionResolver(header)
    }

    private sealed class Case {

        abstract fun assert()

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
