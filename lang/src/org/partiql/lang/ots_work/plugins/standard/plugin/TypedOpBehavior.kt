package org.partiql.lang.ots_work.plugins.standard.plugin

/**
 * Indicates how CAST should behave.
 */
enum class TypedOpBehavior {
    /** The old behavior that ignores type arguments in CAST and IS. */
    LEGACY,

    /**
     * CAST and IS operators respect type parameters.
     *
     * The following behavior is added to `CAST`:
     *
     * - When casting a `DECIMAL(precision, scale)` with a greater scale to a `DECIMAL(precision, scale)` type with a
     * lower scale, rounds [half to even](https://en.wikipedia.org/wiki/Rounding#Round_half_to_even) as needed.
     * - When casting to `CHAR(n)` and `VARCHAR(n)`, if after conversion to unicode string, the value has more unicode
     * codepoints than `n`, truncation is performed.  Trailing spaces (`U+0020`) are fully preserved when casting to
     * `VARCHAR(n)`, but trimmed when casting to `CHAR(n).
     *
     * The following behavior is added to `IS`:
     *
     * - For string type `VARCHAR(n)`, the left-hand side of `IS` must evaluate be a string (not a symbol)
     * where the number of unicode code points is less than or equal `n`.
     * - When casting a `DECIMAL(precision, scale)` with a greater scale to a `DECIMAL(precision, scale)` type with a
     * lower scale, rounds [half to even](https://en.wikipedia.org/wiki/Rounding#Round_half_to_even) as needed.
     * - When casting to `CHAR(n)` and `VARCHAR(n)`, if after conversion to unicode string, the value has more unicode
     * codepoints than `n`, truncation is performed.  Trailing spaces (`U+0020`) are fully preserved when casting to
     * `VARCHAR(n)`, but trimmed when casting to `CHAR(n).
     **/
    HONOR_PARAMETERS
}
