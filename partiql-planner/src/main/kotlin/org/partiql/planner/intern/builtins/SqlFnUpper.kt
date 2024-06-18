package org.partiql.planner.intern.builtins

import org.partiql.planner.metadata.Routine
import org.partiql.types.PType.Kind

/*
 * <fold> is a pair of functions for converting all the lower case and title case characters in a given string to
 * upper case (UPPER) or all the upper case and title case characters to lower case (LOWER).
 */
internal object SqlFnUpper : SqlFn.Definition {

    override fun getVariants(): List<SqlFn> = variants

    @JvmStatic
    private val variants = listOf(
        upper(Kind.CHAR),
        upper(Kind.VARCHAR),
        upper(Kind.STRING),
    )

    @JvmStatic
    private fun upper(string: Kind) = SqlFn(
        name = "upper",
        parameters = listOf(Routine.Parameter("string", string)),
        returnType = string,
        validator = { args -> args[0] }, // type remains the same
    )
}
