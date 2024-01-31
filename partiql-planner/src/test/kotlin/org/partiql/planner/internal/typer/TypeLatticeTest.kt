package org.partiql.planner.internal.typer

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.partiql.value.PartiQLValueExperimental

class TypeLatticeTest {

    @Test
    @Disabled
    fun latticeAsciidocDump() {
        // this test only exists for dumping the type lattice as Asciidoc
        println(toMarkdown(TypeCasts.partiql()))
    }

    @OptIn(PartiQLValueExperimental::class)
    internal fun toAscii(tbl: TypeCasts) = buildString {
        appendLine("|===")
        appendLine()
        // Header
        append("| | ").appendLine(tbl.types.joinToString("| "))
        // Body
        for (t1 in tbl.types) {
            append("| $t1 ")
            for (t2 in tbl.types) {
                val symbol = when (val r = tbl.graph[t1.ordinal][t2.ordinal]) {
                    null -> "X"
                    else -> when (r.castType) {
                        CastType.COERCION -> "⬤"
                        CastType.EXPLICIT -> "◯"
                        CastType.UNSAFE -> "△"
                    }
                }
                append("| $symbol ")
            }
            appendLine()
        }
        appendLine()
        appendLine("|===")
    }

    @OptIn(PartiQLValueExperimental::class)
    internal fun toMarkdown(tbl: TypeCasts) = buildString {
        // Header
        append("|")
        append(tbl.types.joinToString("| "))
        append("|")
        appendLine()
        append("|")
        tbl.types.forEach {
            append("-------|")
        }
        appendLine()
        // Body
        for (t1 in tbl.types) {
            append("| $t1 ")
            for (t2 in tbl.types) {
                val symbol = when (val r = tbl.graph[t1.ordinal][t2.ordinal]) {
                    null -> "X"
                    else -> when (r.castType) {
                        CastType.COERCION -> "⬤"
                        CastType.EXPLICIT -> "◯"
                        CastType.UNSAFE -> "△"
                    }
                }
                append("| $symbol ")
            }
            append("|")
            appendLine()
        }
    }
}
