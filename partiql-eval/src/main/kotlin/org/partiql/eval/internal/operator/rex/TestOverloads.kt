package org.partiql.eval.internal.operator.rex

public class TestOverloads {
    /**
     * XX
     * @param a X
     * @param b X
     * @param c X
     * @return X
     */
    @JvmOverloads
    public fun foo(a: Int? = null, b: Int? = null, c: Int? = null): Int {
        return 1
    }
}
