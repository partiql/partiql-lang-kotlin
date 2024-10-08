package org.partiql.plan

import org.partiql.plan.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface Operation {

    /**
     * PartiQL Query Statement — i.e. SELECT-FROM
     */
    public interface Query : Operation {

        /**
         * Returns the root rex of a PartiQL Query expression.
         */
        public fun getRoot(): Rex
    }
}
