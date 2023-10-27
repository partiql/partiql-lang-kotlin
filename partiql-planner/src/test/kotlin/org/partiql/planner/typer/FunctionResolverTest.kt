package org.partiql.planner.typer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.partiql.planner.Header
import org.partiql.planner.PartiQLHeader
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import javax.print.DocFlavor.STRING

/**
 * As far as testing is concerned, we can stub out all value related things.
 * We may be able to pretty-print with string equals to also simplify things.
 * Only the "types" of expressions matter, we ignore the underlying ops.
 */
@OptIn(PartiQLValueExperimental::class)
class FunctionResolverTest {

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

        val split = FunctionSignature.Scalar(
            name = "split",
            returns = PartiQLValueType.LIST,
            parameters = listOf(
                FunctionParameter("value", PartiQLValueType.STRING),
                FunctionParameter("delimiter", PartiQLValueType.STRING),
            ),
            isNullable = false,
        )

        private val myHeader = object : Header() {

            override val namespace: String = "my_header"

            override val functions: List<FunctionSignature.Scalar> = listOf(
                split
            )
        }

        private val resolver = FnResolver(listOf(PartiQLHeader, myHeader))
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
