package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
public interface RexPath : Rex {

    public fun getRoot(): Rex

    /**
     * TODO DOCUMENTATION
     */
    public interface Index : RexPath {
        public fun getIndex(): Rex
    }

    /**
     * TODO DOCUMENTATION
     */
    public interface Key : RexPath {
        public fun getKey(): Rex
    }

    /**
     * TODO DOCUMENTATION
     */
    public interface Symbol : RexPath {
        public fun getSymbol(): String
    }
}
