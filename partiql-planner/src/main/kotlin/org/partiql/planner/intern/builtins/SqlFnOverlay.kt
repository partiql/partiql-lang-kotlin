package org.partiql.planner.intern.builtins

import org.partiql.planner.metadata.Routine
import org.partiql.types.PType.Kind

internal object SqlFnOverlay : SqlFn.Definition {

    override fun getVariants(): List<SqlFn> = variants

    @JvmStatic
    private val variants = listOf(
        overlay2(Kind.CHAR),
        overlay3(Kind.CHAR),
        overlay2(Kind.VARCHAR),
        overlay3(Kind.VARCHAR),
        overlay2(Kind.STRING),
        overlay3(Kind.STRING),
    )

    @JvmStatic
    private fun overlay2(string: Kind) = SqlFn(
        name = "overlay",
        parameters = listOf(
            Routine.Parameter("replacement", string),
            Routine.Parameter("string", string),
        ),
        returnType = string,
    )

    @JvmStatic
    private fun overlay3(string: Kind) = SqlFn(
        name = "overlay",
        parameters = listOf(
            Routine.Parameter("replacement", string),
            Routine.Parameter("string", string),
            Routine.Parameter("count", Kind.INT),
        ),
        returnType = string
    )
}
