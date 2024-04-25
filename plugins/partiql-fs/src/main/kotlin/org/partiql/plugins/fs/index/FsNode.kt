package org.partiql.plugins.fs.index

import org.partiql.types.StaticType
import java.io.File
import java.nio.file.Path

/**
 * The Fs equivalent of an inode.
 */
internal sealed interface FsNode {

    val name: String
    val path: Path

    data class Scope(
        override val name: String,
        override val path: Path,
        val children: List<FsNode>,
    ) : FsNode {

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
        override val path: Path,
        val shape: StaticType,
        val data: File? = null,
    ) : FsNode {

        override fun toString(): String = "$name: $shape"
    }
}
