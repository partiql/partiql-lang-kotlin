package org.partiql.plan.logical

public interface Statement : Node {

    /**
     * A query statement
     */
    public interface Query : Statement {

        public companion object {
            @JvmStatic
            public fun builder(): Unit = TODO("Statement builder not implemented")
        }
    }
}
