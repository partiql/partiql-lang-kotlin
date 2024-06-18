package org.partiql.planner.intern.builtins

import org.partiql.planner.metadata.Routine
import org.partiql.types.PType
import org.partiql.types.PType.Kind

/**
 * <like predicate> uses the triadic operator LIKE (or the inverse, NOT LIKE), operating on three character strings
 * and returning a Boolean. LIKE determines whether or not a character string ‘‘matches’’ a given ‘‘pattern’’
 * (also a character string).
 *
 * E061-04 — LIKE predicate.
 * E061-05 — LIKE predicate: ESCAPE clause.
 *
 * Defined
 *  - 4.2.2.2 Other operators involving character strings
 *  - 4.2.2.3 Operations involving large object character strings
 *  - 4.3.2.2 Other operators involving binary strings
 */
internal object SqlFnLike : SqlFn.Definition {

    override fun getVariants(): List<SqlFn> = variants

    @JvmStatic
    private val variants = listOf(
        characterLike(Kind.CHAR),
        characterLike(Kind.VARCHAR),
        characterLike(Kind.STRING),
        characterLike(Kind.CLOB),
        // octetLike(Kind.BIT),
        // octetLike(Kind.VARBIT),
        // octetLike(Kind.BLOB),
    )

    /**
     * Note that `\` is inserted as the default ESCAPE value.
     */
    @JvmStatic
    private fun characterLike(string: Kind) = SqlFn(
        name = "like",
        parameters = listOf(
            Routine.Parameter("string", string),
            Routine.Parameter("pattern", Kind.STRING),
            Routine.Parameter("escape", Kind.CHAR),
        ),
        returnType = Kind.BOOL,
        validator = {
            val escape = it[2]
            if (escape.kind != Kind.CHAR || escape.maxLength != 1) {
                error("ESCAPE character must be")
            }
            PType.typeBool()
        }
    )
}
