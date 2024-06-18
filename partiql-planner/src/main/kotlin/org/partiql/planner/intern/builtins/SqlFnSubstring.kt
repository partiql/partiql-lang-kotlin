package org.partiql.planner.intern.builtins

import org.partiql.planner.metadata.Routine
import org.partiql.types.PType.Kind

/**
 * <character substring function> is a triadic function, SUBSTRING, that returns a string extracted from a given
 * string according to a given numeric starting position and a given numeric length.
 */
internal object SqlFnSubstring : SqlFn.Definition {

    override fun getVariants(): List<SqlFn> = variants

    @JvmStatic
    private val variants = listOf(
        substring2(Kind.CHAR),
        substring3(Kind.CHAR),
        substring2(Kind.VARCHAR),
        substring3(Kind.VARCHAR),
        substring2(Kind.STRING),
        substring3(Kind.STRING),
    )

    @JvmStatic
    private fun substring2(string: Kind) = SqlFn(
        name = "substring",
        parameters = listOf(
            Routine.Parameter("string", string),
            Routine.Parameter("start", Kind.INT),
        ),
        returnType = string,
    )

    @JvmStatic
    private fun substring3(string: Kind) = SqlFn(
        name = "substring",
        parameters = listOf(
            Routine.Parameter("string", string),
            Routine.Parameter("start", Kind.INT),
            Routine.Parameter("end", Kind.INT),
        ),
        returnType = string
    )
}
