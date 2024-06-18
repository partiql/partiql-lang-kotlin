package org.partiql.planner.intern.builtins

import org.partiql.planner.metadata.Routine
import org.partiql.types.PType.Kind

/**
 * <length expression> returns the length of a given character string, as an exact numeric value,
 * in characters, octets, or bits according to the choice of function.
 */
internal object SqlFnCharLength : SqlFn.Definition {

    override fun getVariants() = variants

    @JvmStatic
    private val variants = listOf(
        charLength(Kind.CHAR),
        charLength(Kind.VARCHAR),
        charLength(Kind.STRING),
    )

    /**
     * `character_length` is normalized to `char_length`.
     */
    @JvmStatic
    private fun charLength(string: Kind) = SqlFn(
        name = "char_length",
        parameters = listOf(Routine.Parameter("string", string)),
        returnType = Kind.INT,
    )
}
