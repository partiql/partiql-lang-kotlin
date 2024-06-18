package org.partiql.planner.intern.builtins

import org.partiql.planner.metadata.Routine
import org.partiql.types.PType.Kind

/*
 * <fold> is a pair of functions for converting all the lower case and title case characters in a given string to
 * upper case (UPPER) or all the upper case and title case characters to lower case (LOWER).
 */
internal object SqlFnLower : SqlFn.Definition {

    override fun getVariants(): List<SqlFn> = variants

    @JvmStatic
    private val variants = listOf(
        lower(Kind.CHAR),
        lower(Kind.VARCHAR),
        lower(Kind.STRING),
    )

    @JvmStatic
    private fun lower(string: Kind) = SqlFn(
        name = "lower",
        parameters = listOf(Routine.Parameter("string", string)),
        returnType = string,
        validator = { args -> args[0] }, // type remains the same
    )
}
