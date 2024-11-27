package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.rex.Rex;

/**
 * TODO DOCUMENTATION
 */
public interface Operation {

    /**
     * PartiQL Query Statement — i.e. SELECT-FROM
     */
    public interface Query extends Operation {

        /**
         * Returns the root expression of the query.
         */
        @NotNull
        public Rex getRex();
    }
}
