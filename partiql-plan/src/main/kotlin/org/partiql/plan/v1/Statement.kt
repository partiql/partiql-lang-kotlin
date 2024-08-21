package org.partiql.plan.v1

import org.partiql.plan.v1.operator.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface Statement {

    /**
     * PartiQL Query Statement — i.e. SELECT-FROM
     */
    public interface Query : Statement {

        /**
         * Returns the root rex of a PartiQL Query expression.
         */
        public fun getRoot(): Rex
    }
}
