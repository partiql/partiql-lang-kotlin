package org.partiql.spi.connector.sql

import net.pearx.kasechange.toPascalCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValueExperimental
import java.io.File

const val imports = """
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValueExperimental
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
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object %s : FnScalar {

    override val signature = FnSignature(
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
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object %s : Agg {

    override val signature = AggSignature(
        name = "%s",
        returns = %s,
        parameters = listOf(%s),
        isNullable = %b,
        isDecomposable = %b
    )

    override fun accumulator(): Agg.Accumulator {
        TODO("Aggregation %s not implemented")
    }
}
"""

@Disabled
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
class HeaderCodeGen {

    @Test
    fun scalars() {
        generateFns("package org.partiql.spi.connector.sql.internal.builtins", TEMPLATE_SCALAR, "Fn", SqlHeader.fns)
    }

    @Test
    fun aggregations() {
        generateAggs("package org.partiql.spi.connector.sql.internal.builtins", TEMPLATE_AGG, "Agg", SqlHeader.aggs)
    }

    /**
     * Writes function implementation to file, prints list of classes
     */
    private fun generateFns(
        packageName: String,
        template: String,
        prefix: String,
        signatures: List<FnSignature>,
    ) {
        val clazzes = mutableListOf<String>()
        val funcs = signatures.groupBy { it.name }
        val pathToDir = "./src/main/kotlin/org/partiql/spi/connector/sql/builtins"
        for ((name, fns) in funcs) {
            val pre = "${prefix}_$name".toPascalCase()
            val file = File("$pathToDir/$pre.kt")
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

    /**
     * Writes function implementation to file, prints list of classes
     */
    private fun generateAggs(
        packageName: String,
        template: String,
        prefix: String,
        signatures: List<AggSignature>,
    ) {
        val clazzes = mutableListOf<String>()
        val funcs = signatures.groupBy { it.name }
        val pathToDir = "./src/main/kotlin/org/partiql/spi/connector/sql/builtins"
        for ((name, fns) in funcs) {
            val pre = "${prefix}_$name".toPascalCase()
            val file = File("$pathToDir/$pre.kt")
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

    @OptIn(FnExperimental::class)
    private fun toParams(clazz: String, fn: FnSignature): Array<out Any?> {
        val snake = fn.name
        val returns = fn.returns.name
        val parameters = fn.parameters.mapIndexed { i, p ->
            "FnParameter(\"${p.name}\", ${p.type.name})"
        }.joinToString(",\n", postfix = ",")
        return arrayOf(clazz, snake, returns, parameters, fn.isNullCall, fn.isNullable, snake)
    }

    @OptIn(FnExperimental::class)
    private fun toParams(clazz: String, agg: AggSignature): Array<out Any?> {
        val snake = agg.name
        val returns = agg.returns.name
        var parameters = ""
        for (p in agg.parameters) {
            parameters += "FnParameter(\"${p.name}\", ${p.type.name}),\n"
        }
        return arrayOf(clazz, snake, returns, parameters, agg.isNullable, agg.isDecomposable, snake)
    }
}
