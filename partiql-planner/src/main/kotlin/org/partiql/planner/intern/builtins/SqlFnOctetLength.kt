package org.partiql.planner.intern.builtins

import org.partiql.planner.metadata.Routine
import org.partiql.types.PType.Kind

/**
 * <length expression> returns the length of a given character string, as an exact numeric value,
 * in characters, octets, or bits according to the choice of function.
 */
internal object SqlFnOctetLength : SqlFn.Definition {

    override fun getVariants() = variants

    @JvmStatic
    private val variants = listOf(
        octetLength(Kind.CHAR),
        octetLength(Kind.VARCHAR),
        octetLength(Kind.STRING),
    )

    @JvmStatic
    private fun octetLength(string: Kind) = SqlFn(
        name = "octet_length",
        parameters = listOf(Routine.Parameter("string", string)),
        returnType = Kind.INT,
    )
}
