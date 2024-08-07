package org.partiql.plan.operator.rel

import org.partiql.plan.Schema
import org.partiql.plan.builder.RelBuilder
import org.partiql.plan.operator.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface Rel {

    public fun getInputs(): List<Rel>

    public fun getSchema(): Schema

    public fun isOrdered(): Boolean

    public fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R

    /**
     * Static build methods for the builder .
     */
    public companion object {

        /**
         * TODO
         */
        @JvmStatic
        public fun scan(rex: Rex): Rel = RelBuilder.scan(rex).build()

        /**
         * TODO
         */
        @JvmStatic
        public fun scanIndexed(rex: Rex): Rel = RelBuilder.scanIndexed(rex).build()

        /**
         * TODO
         */
        @JvmStatic
        public fun unpivot(rex: Rex): Rel = RelBuilder.unpivot(rex).build()
    }
}
