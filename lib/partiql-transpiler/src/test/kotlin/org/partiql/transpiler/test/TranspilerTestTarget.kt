package org.partiql.transpiler.test

public abstract class TranspilerTestTarget(
    public val name: String,
    public val suite: String,
) {

    /**
     * Peform target-specific assertion.
     *
     * @param test
     */
    public abstract fun assert(test: TranspilerTest)
}
