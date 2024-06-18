package org.partiql.planner.intern.builtins

import org.partiql.planner.metadata.Routine
import org.partiql.types.PType.Kind


internal object SqlFnSimilar : SqlFn.Definition {

    override fun getVariants(): List<SqlFn> = variants

    @JvmStatic
    private val variants = listOf(
        similar(Kind.CHAR),
        similar(Kind.VARCHAR),
        similar(Kind.STRING),
        similar(Kind.CLOB),
    )

    /**
     * Note that `\` is inserted as the default ESCAPE value.
     */
    @JvmStatic
    private fun similar(string: Kind) = SqlFn(
        name = "similar",
        parameters = listOf(
            Routine.Parameter("string", string),
            Routine.Parameter("pattern", Kind.STRING),
            Routine.Parameter("escape", Kind.CHAR),
        ),
        returnType = Kind.BOOL,
    )
}
