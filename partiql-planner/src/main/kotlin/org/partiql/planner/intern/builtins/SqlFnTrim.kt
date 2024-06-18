package org.partiql.planner.intern.builtins

import org.partiql.planner.metadata.Routine
import org.partiql.types.PType.Kind

/**
 * <trim function> is a function that returns its first string argument with leading and/or trailing pad characters
 * removed. The second argument indicates whether leading, or trailing, or both leading and trailing pad characters
 * should be removed. The third argument specifies the pad character that is to be removed.
 */
internal object SqlFnTrim : SqlFn.Definition {

    override fun getVariants(): List<SqlFn> = variants

    @JvmStatic
    private val variants = mutableListOf<SqlFn>().apply {
        val types = listOf(Kind.CHAR, Kind.VARCHAR, Kind.STRING)
        for (t in types) {
            add(trim(t))
            add(trim(t, t))
            add(trimLeading(t))
            add(trimLeading(t, t))
            add(trimTrailing(t))
            add(trimTrailing(t, t))
        }
    }

    @JvmStatic
    private fun trim(string: Kind) = SqlFn(
        name = "trim",
        parameters = listOf(Routine.Parameter("string", string)),
        returnType = string,
    )

    @JvmStatic
    private fun trimLeading(string: Kind) = SqlFn(
        name = "trim_leading",
        parameters = listOf(Routine.Parameter("string", string)),
        returnType = string,
    )

    @JvmStatic
    private fun trimTrailing(string: Kind) = SqlFn(
        name = "trim_trailing",
        parameters = listOf(Routine.Parameter("string", string)),
        returnType = string,
    )

    @JvmStatic
    private fun trim(string: Kind, chars: Kind) = SqlFn(
        name = "trim",
        parameters = listOf(Routine.Parameter("string", string), Routine.Parameter("chars", chars)),
        returnType = string,
    )

    @JvmStatic
    private fun trimLeading(string: Kind, chars: Kind) = SqlFn(
        name = "trim_leading",
        parameters = listOf(Routine.Parameter("string", string), Routine.Parameter("chars", chars)),
        returnType = string,
    )

    @JvmStatic
    private fun trimTrailing(string: Kind, chars: Kind) = SqlFn(
        name = "trim_trailing",
        parameters = listOf(Routine.Parameter("string", string), Routine.Parameter("chars", chars)),
        returnType = string,
    )
}