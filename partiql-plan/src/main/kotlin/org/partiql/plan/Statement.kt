package org.partiql.plan

import org.partiql.plan.operator.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface Statement {

    /**
     * PartiQL Query Statement — i.e. SELECT-FROM
     */
    public interface Query {

        /**
         * Returns the root rex of a PartiQL Query expression.
         */
        public fun getRoot(): Rex
    }
}
