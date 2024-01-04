package org.partiql.plugin

import net.pearx.kasechange.toPascalCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import java.io.File

const val imports = """
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*
"""

/**
 * 0 -> Pascal case name
 * 1 —> Snake case name
 * 2 —> Return Type
 * 3 —> Parameter list as string
 * 4 —> isNullCall
 * 5 -> isNullable
 * 6 —> Snake case name
 */
const val TEMPLATE_SCALAR = """
@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object %s : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "%s",
        returns = %s,
        parameters = listOf(%s),
        isNullCall = %b,
        isNullable = %b,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function %s not implemented")
    }
}
"""

/**
 * 0 -> Pascal case name
 * 1 —> Snake case name
 * 2 —> Return Type
 * 3 —> Parameter list as string
 * 4 -> isNullable
 * 5 —> isDecomposable
 * 6 —> Snake case name
 */
const val TEMPLATE_AGG = """
@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object %s : PartiQLFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "%s",
        returns = %s,
        parameters = listOf(%s),
        isNullable = %b,
        isDecomposable = %b
    )

    override fun accumulator(): PartiQLFunction.Accumulator {
        TODO("Aggregation %s not implemented")
    }
}
"""

@Disabled
@OptIn(PartiQLValueExperimental::class)
class HeaderTest {

    @Test
    fun scalars() {
        generate("package org.partiql.plugin.internal.fn.scalar", TEMPLATE_SCALAR, "Fn", PartiQLHeader.functions)
        generate("package org.partiql.plugin.internal.fn.scalar", TEMPLATE_SCALAR, "Fn", PartiQLHeader.operators)
    }

    @Test
    fun aggregations() {
        generate("package org.partiql.plugin.internal.fn.agg", TEMPLATE_AGG, "Agg", PartiQLHeader.aggregations)
    }

    /**
     * Writes function implementation to file, prints list of classes
     */
    private fun generate(
        packageName: String,
        template: String,
        prefix: String,
        signatures: List<FunctionSignature>,
    ) {
        val clazzes = mutableListOf<String>()
        val funcs = signatures.groupBy { it.name }
        for ((name, fns) in funcs) {
            val pre = "${prefix}_$name".toPascalCase()
            val file = File("/Users/howero/Desktop/out/$pre.kt")
            file.printWriter().use {
                it.appendLine("// ktlint-disable filename")
                it.appendLine("@file:Suppress(\"ClassName\")")
                it.appendLine()
                it.appendLine(packageName)
                it.appendLine()
                it.appendLine(imports)
                it.appendLine()
                fns.forEach { sig ->
                    val clazz = "${prefix}_${sig.specific}"
                    val params = toParams(clazz, sig)
                    val code = String.format(template, *params)
                    it.appendLine(code)
                    it.appendLine()
                    clazzes.add(clazz)
                }
            }
        }
        println("-- GENERATED")
        println("listOf(${clazzes.joinToString()})")
        println()
    }

    private fun toParams(clazz: String, fn: FunctionSignature) = when (fn) {
        is FunctionSignature.Aggregation -> toParams(clazz, fn)
        is FunctionSignature.Scalar -> toParams(clazz, fn)
    }

    private fun toParams(clazz: String, fn: FunctionSignature.Scalar): Array<out Any?> {
        val snake = fn.name
        val returns = fn.returns.name
        val parameters = fn.parameters.mapIndexed { i, p ->
            "FunctionParameter(\"${p.name}\", ${p.type.name})"
        }.joinToString(",\n", postfix = ",")
        return arrayOf(clazz, snake, returns, parameters, fn.isNullCall, fn.isNullable, snake)
    }

    private fun toParams(clazz: String, fn: FunctionSignature.Aggregation): Array<out Any?> {
        val snake = fn.name
        val returns = fn.returns.name
        var parameters = ""
        for (p in fn.parameters) {
            parameters += "FunctionParameter(\"${p.name}\", ${p.type.name}),\n"
        }
        return arrayOf(clazz, snake, returns, parameters, fn.isNullable, fn.isDecomposable, snake)
    }
}
