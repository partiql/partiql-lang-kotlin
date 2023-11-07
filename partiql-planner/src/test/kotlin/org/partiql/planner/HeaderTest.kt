package org.partiql.planner

import kotlinx.html.TABLE
import kotlinx.html.body
import kotlinx.html.classes
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.html
import kotlinx.html.main
import kotlinx.html.stream.appendHTML
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.tr
import org.junit.jupiter.api.Test
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental

class HeaderTest {

    @Test
    fun print() {
        // println(PartiQLHeader)
        val header = PartiQLHeader
        //
        val scalars = (header.functions + header.operators)
            .groupBy { it.name }
            .entries.sortedBy { it.key }
        val aggs = (header.aggregations)
            .groupBy { it.name }
            .entries.sortedBy { it.key }
        // build doc
        val sb = StringBuilder()
        sb.appendHTML().html {
            body {
                main {
                    classes += "container"
                    h2 {
                        +"Functions"
                    }
                    h3 {
                        +"Scalar"
                    }
                    table {
                        tr {
                            th { +"Feature" }
                            th { +"Name" }
                            th { +"Args" }
                            th { +"Return" }
                            th { +"Props" }
                        }
                        for ((i, e) in scalars.withIndex()) {
                            for ((j, f) in e.value.withIndex()) {
                                this.add(f, i, j)
                            }
                        }
                    }
                    h3 {
                        +"Aggregation"
                    }
                    table {
                        tr {
                            th { +"Feature" }
                            th { +"Name" }
                            th { +"Args" }
                            th { +"Return" }
                            th { +"Props" }
                        }
                        for ((i, e) in aggs.withIndex()) {
                            for ((j, f) in e.value.withIndex()) {
                                this.add(f, i, j)
                            }
                        }
                    }
                }
            }
        }
        println(sb)
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun TABLE.add(f: FunctionSignature.Scalar, i: Int, j: Int) {
        val key = "FS-%03d-%02d".format(i, j)
        val name = f.name
        val args = f.parameters.joinToString { it.type.toString().lowercase() }
        val returns = f.returns.toString().lowercase()
        val props = props(f)
        tr {
            td { +key }
            td { +name }
            td { +args }
            td { +returns }
            td { +props }
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun TABLE.add(f: FunctionSignature.Aggregation, i: Int, j: Int) {
        val key = "FA-%03d-%02d".format(i, j)
        val name = f.name
        val args = f.parameters.joinToString { it.type.toString().lowercase() }
        val returns = f.returns.toString().lowercase()
        val props = props(f)
        tr {
            td { +key }
            td { +name }
            td { +args }
            td { +returns }
            td { +props }
        }
    }

    private fun props(f: FunctionSignature.Scalar): String {
        val props = mutableListOf<String>()
        if (f.isNullable) {
            props += "nullable"
        }
        if (f.isMissable) {
            props += "missable"
        }
        if (f.isNullCall) {
            props += "nullcall"
        }
        return props.joinToString()
    }

    private fun props(f: FunctionSignature.Aggregation): String {
        val props = mutableListOf<String>()
        if (f.isNullable) {
            props += "nullable"
        }
        if (f.isDecomposable) {
            props += "decomposable"
        }
        return props.joinToString()
    }
}
