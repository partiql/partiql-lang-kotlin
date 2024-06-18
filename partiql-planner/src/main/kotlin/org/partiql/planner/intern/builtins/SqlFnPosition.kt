package org.partiql.planner.intern.builtins

import org.partiql.planner.metadata.Routine
import org.partiql.types.PType.Kind

/**
 * <position expression> determines the first position, if any, at which one string, S1, occurs within another, S2.
 * If S1 is of length zero, then it occurs at position 1 (one) for any value of S2. If S1 does not occur in S2,
 * then zero is returned. The declared type of a <position expression> is exact numeric.
 */
internal object SqlFnPosition : SqlFn.Definition {

    override fun getVariants(): List<SqlFn> = variants

    @JvmStatic
    private val variants = listOf(
        position(Kind.CHAR, Kind.CHAR),
        position(Kind.VARCHAR, Kind.VARCHAR),
        position(Kind.STRING, Kind.STRING),
        position(Kind.CLOB, Kind.CLOB),
    )

    @JvmStatic
    private fun position(s1: Kind, s2: Kind) = SqlFn(
        name = "position",
        parameters = listOf(
            Routine.Parameter("s1", s1),
            Routine.Parameter("s2", s2),
        ),
        returnType = Kind.INT,
    )
}
