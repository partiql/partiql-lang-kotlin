package org.partiql.plan

import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexType

/**
 * TODO DOCUMENTATION
 */
public interface Operation {

    /**
     * PartiQL Query Statement — i.e. SELECT-FROM
     */
    public interface Query : Operation {

        /**
         * Returns the root expression of the query.
         */
        public fun getRex(): Rex

        /**
         * Returns the type of the root expression of the query.
         */
        public fun getType(): RexType = getRex().getType()
    }
}
