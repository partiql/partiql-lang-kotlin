package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelExcludeStep : Rel {

    public fun getSteps(): List<RelExcludeStep>

    /**
     * TODO DOCUMENTATION
     */
    public interface Index : RelExcludeStep {
        public fun getIndex(): Int
    }

    /**
     * TODO DOCUMENTATION
     */
    public interface Key : RelExcludeStep {
        public fun getKey(): String
    }

    /**
     * TODO DOCUMENTATION
     */
    public interface Symbol : RelExcludeStep {
        public fun getSymbol(): String
    }

    /**
     * TODO DOCUMENTATION
     */
    public interface StructWildcard : RelExcludeStep

    /**
     * TODO DOCUMENTATION
     */
    public interface CollectionWildcard : RelExcludeStep
}
