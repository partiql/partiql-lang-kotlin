package org.partiql.planner.intern.builtins

import org.partiql.planner.intern.SqlTypes
import org.partiql.planner.intern.ptype.PType
import org.partiql.planner.metadata.Fn
import org.partiql.planner.metadata.Operator

/**
 * SQL-99 4.2.2 — Operators involving character strings.
 */
internal object SqlStringBuiltins {

    @JvmStatic
    internal val ALL = listOf(
        Concat,
        Substring,
        Overlay,
        Fold,
        Trim,
        Length,
        Position,
        Like,
        Similar,
    )

    internal object Concat : SqlDefinition.Operator {

        override fun getVariants(): List<Operator> = variants

        @JvmStatic
        private val variants = SqlTypes.strings.map { concatenation(it, it) }

        @JvmStatic
        private fun concatenation(lhs: PType.Kind, rhs: PType.Kind) = Operator.create(
            name = "concat",
            symbol = "||",
            lhs = lhs,
            rhs = rhs,
            returnType = rhs,
            validator = { args ->
                val l = args[0].getMaxLength() + args[1].getMaxLength()
                PType(rhs, l)
            }
        )
    }

    /**
     * <character substring function> is a triadic function, SUBSTRING, that returns a string extracted from a given
     * string according to a given numeric starting position and a given numeric length.
     */
    internal object Substring : SqlDefinition.Fn {

        override fun getVariants(): List<Fn> = variants

        @JvmStatic
        private val variants = SqlTypes.strings.map { substring2(it) } + SqlTypes.strings.map { substring3(it) }

        @JvmStatic
        private fun substring2(string: PType.Kind) = Fn.scalar(
            name = "substring",
            parameters = listOf(
                Fn.Parameter("string", string),
                Fn.Parameter("start", PType.Kind.INT),
            ),
            returnType = string
        )

        @JvmStatic
        private fun substring3(string: PType.Kind) = Fn.scalar(
            name = "substring",
            parameters = listOf(
                Fn.Parameter("string", string),
                Fn.Parameter("start", PType.Kind.INT),
                Fn.Parameter("end", PType.Kind.INT),
            ),
            returnType = string
        )
    }

    internal object Overlay : SqlDefinition.Fn {

        override fun getVariants(): List<Fn> = variants

        @JvmStatic
        private val variants = mutableListOf<Fn>().apply {
            for (t in SqlTypes.strings) {
                add(overlay2(t))
                add(overlay3(t))
            }
        }

        @JvmStatic
        private fun overlay2(string: PType.Kind) = Fn.scalar(
            name = "overlay",
            parameters = listOf(
                Fn.Parameter("replacement", string),
                Fn.Parameter("string", string),
            ),
            returnType = string
        )

        @JvmStatic
        private fun overlay3(string: PType.Kind) = Fn.scalar(
            name = "overlay",
            parameters = listOf(
                Fn.Parameter("replacement", string),
                Fn.Parameter("string", string),
                Fn.Parameter("count", PType.Kind.INT),
            ),
            returnType = string
        )
    }

    /*
     * <fold> is a pair of functions for converting all the lower case and title case characters in a given string to
     * upper case (UPPER) or all the upper case and title case characters to lower case (LOWER).
     */
    internal object Fold : SqlDefinition.Fn {

        override fun getVariants(): List<Fn> = variants

        @JvmStatic
        private val variants = SqlTypes.strings.map { upper(it) } + SqlTypes.strings.map { lower(it) }

        @JvmStatic
        private fun upper(string: PType.Kind): Fn.Scalar = Fn.scalar(
            name = "upper",
            parameters = listOf(Fn.Parameter("string", string)),
            returnType = string,
            validator = { args -> args[0] },
        )

        @JvmStatic
        private fun lower(string: PType.Kind): Fn.Scalar = Fn.scalar(
            name = "lower",
            parameters = listOf(Fn.Parameter("string", string)),
            returnType = string,
            validator = { args -> args[0] },
        )
    }

    /**
     * <trim function> is a function that returns its first string argument with leading and/or trailing pad characters
     * removed. The second argument indicates whether leading, or trailing, or both leading and trailing pad characters
     * should be removed. The third argument specifies the pad character that is to be removed.
     */
    internal object Trim : SqlDefinition.Fn {

        override fun getVariants(): List<Fn> = variants

        @JvmStatic
        private val variants = mutableListOf<Fn>().apply {
            for (t in SqlTypes.strings) {
                add(trim(t))
                add(trim(t, t))
                add(trimLeading(t))
                add(trimLeading(t, t))
                add(trimTrailing(t))
                add(trimTrailing(t, t))
            }
        }

        @JvmStatic
        private fun trim(string: PType.Kind): Fn = Fn.scalar(
            name = "trim",
            parameters = listOf(Fn.Parameter("string", string)),
            returnType = string,
        )

        @JvmStatic
        private fun trimLeading(string: PType.Kind): Fn = Fn.scalar(
            name = "trim_leading",
            parameters = listOf(Fn.Parameter("string", string)),
            returnType = string,
        )

        @JvmStatic
        private fun trimTrailing(string: PType.Kind): Fn = Fn.scalar(
            name = "trim_trailing",
            parameters = listOf(Fn.Parameter("string", string)),
            returnType = string,
        )

        @JvmStatic
        private fun trim(string: PType.Kind, chars: PType.Kind): Fn = Fn.scalar(
            name = "trim",
            parameters = listOf(Fn.Parameter("string", string), Fn.Parameter("chars", chars)),
            returnType = string,
        )

        @JvmStatic
        private fun trimLeading(string: PType.Kind, chars: PType.Kind): Fn = Fn.scalar(
            name = "trim_leading",
            parameters = listOf(Fn.Parameter("string", string), Fn.Parameter("chars", chars)),
            returnType = string,
        )

        @JvmStatic
        private fun trimTrailing(string: PType.Kind, chars: PType.Kind): Fn = Fn.scalar(
            name = "trim_trailing",
            parameters = listOf(Fn.Parameter("string", string), Fn.Parameter("chars", chars)),
            returnType = string,
        )
    }

    /**
     * <length expression> returns the length of a given character string, as an exact numeric value,
     * in characters, octets, or bits according to the choice of function.
     */
    internal object Length : SqlDefinition.Fn {

        override fun getVariants(): List<Fn> = variants

        @JvmStatic
        private val variants = mutableListOf<Fn>().apply {
            for (t in SqlTypes.strings) {
                add(charLength(t))
                add(octetLength(t))
                add(bitLength(t))
            }
        }

        /**
         * `character_length` is normalized to `char_length`.
         */
        @JvmStatic
        private fun charLength(string: PType.Kind): Fn = Fn.scalar(
            name = "char_length",
            parameters = listOf(Fn.Parameter("string", string)),
            returnType = PType.Kind.INT,
        )

        @JvmStatic
        private fun octetLength(string: PType.Kind): Fn = Fn.scalar(
            name = "octet_length",
            parameters = listOf(Fn.Parameter("string", string)),
            returnType = PType.Kind.INT,
        )

        @JvmStatic
        private fun bitLength(string: PType.Kind): Fn = Fn.scalar(
            name = "bit_length",
            parameters = listOf(Fn.Parameter("string", string)),
            returnType = PType.Kind.INT,
        )
    }

    /**
     * <position expression> determines the first position, if any, at which one string, S1, occurs within another, S2.
     * If S1 is of length zero, then it occurs at position 1 (one) for any value of S2. If S1 does not occur in S2,
     * then zero is returned. The declared type of a <position expression> is exact numeric.
     */
    internal object Position : SqlDefinition.Fn {

        override fun getVariants(): List<Fn> = variants

        @JvmStatic
        private val variants = SqlTypes.strings.map { position(it, it) }

        @JvmStatic
        private fun position(s1: PType.Kind, s2: PType.Kind): Fn.Scalar = Fn.scalar(
            name = "position",
            parameters = listOf(
                Fn.Parameter("s1", s1),
                Fn.Parameter("s2", s2),
            ),
            returnType = PType.Kind.INT,
        )
    }

    /**
     * <like predicate> uses the triadic operator LIKE (or the inverse, NOT LIKE), operating on three character strings
     * and returning a Boolean. LIKE determines whether or not a character string ‘‘matches’’ a given ‘‘pattern’’
     * (also a character string).
     */
    internal object Like : SqlDefinition.Fn {

        override fun getVariants(): List<Fn> = variants

        @JvmStatic
        private val variants = mutableListOf<Fn>().apply {
            for (t in SqlTypes.strings) {
                add(like(t, t))
                add(like(t, t, t))
            }
        }

        @JvmStatic
        private fun like(string: PType.Kind, pattern: PType.Kind): Fn.Scalar = Fn.scalar(
            name = "like",
            parameters = listOf(
                Fn.Parameter("string", string),
                Fn.Parameter("pattern", pattern),
            ),
            returnType = PType.Kind.BOOL,
        )

        @JvmStatic
        private fun like(string: PType.Kind, pattern: PType.Kind, escape: PType.Kind): Fn.Scalar = Fn.scalar(
            name = "like",
            parameters = listOf(
                Fn.Parameter("string", string),
                Fn.Parameter("pattern", pattern),
                Fn.Parameter("escape", escape),
            ),
            returnType = PType.Kind.BOOL,
        )
    }

    internal object Similar : SqlDefinition.Fn {

        override fun getVariants(): List<Fn> = variants

        @JvmStatic
        private val variants = mutableListOf<Fn>().apply {
            for (t in SqlTypes.strings) {
                add(similar(t, t))
                add(similar(t, t, t))
            }
        }

        @JvmStatic
        private fun similar(string: PType.Kind, pattern: PType.Kind): Fn.Scalar = Fn.scalar(
            name = "similar",
            parameters = listOf(
                Fn.Parameter("string", string),
                Fn.Parameter("pattern", pattern),
            ),
            returnType = PType.Kind.BOOL,
        )

        @JvmStatic
        private fun similar(string: PType.Kind, pattern: PType.Kind, escape: PType.Kind): Fn.Scalar = Fn.scalar(
            name = "similar",
            parameters = listOf(
                Fn.Parameter("string", string),
                Fn.Parameter("pattern", pattern),
                Fn.Parameter("escape", escape),
            ),
            returnType = PType.Kind.BOOL,
        )
    }
}
