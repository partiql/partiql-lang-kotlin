package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.rex.Rex;

/**
 * A PartiQL statement action within a plan.
 */
public interface Action {

    /**
     * PartiQL Query Statement — i.e. SELECT-FROM
     */
    public interface Query extends Action {

        /**
         * Returns the root expression of the query.
         */
        @NotNull
        public Rex getRex();
    }
}
