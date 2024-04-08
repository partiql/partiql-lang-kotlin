package org.partiql.plugins.kollider.index

import org.partiql.types.StaticType
import java.io.File

/**
 * The Kollider equivalent of an inode.
 */
internal sealed interface KNode {

    val name: String

    data class Scope(
        override val name: String,
        val children: List<KNode>,
    ) : KNode {

        /**
         * TODO actually tree print like https://github.com/partiql/partiql-lang-kotlin/blob/main/partiql-plan/src/main/kotlin/org/partiql/plan/debug/PlanPrinter.kt
         *
         * @return
         */
        override fun toString(): String = buildString {
            appendLine("| $name")
            for (child in children) {
                append("|--").appendLine(child)
            }
        }
    }

    data class Obj(
        override val name: String,
        val shape: StaticType,
        val data: File? = null,
    ) : KNode {

        override fun toString(): String = "$name: $shape"
    }
}
