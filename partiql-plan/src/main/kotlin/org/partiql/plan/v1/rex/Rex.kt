package org.partiql.plan.v1.rex

import org.partiql.eval.value.Datum
import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface Rex {

    public fun getType(): PType

    public fun getOperands(): List<Rex>

    public fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R

    /**
     * TODO add more literals once Datum is expanded with more factory methods.
     */
    public companion object {

        @JvmStatic
        public fun lit(value: Boolean): Rex = lit(Datum.bool(value))

        @JvmStatic
        public fun lit(value: Int): Rex = lit(Datum.integer(value))

        @JvmStatic
        public fun lit(value: Long): Rex = lit(Datum.bigint(value))

        @JvmStatic
        public fun lit(value: String): Rex = lit(Datum.string(value))

        @JvmStatic
        public fun lit(value: Datum): Rex = object : RexLit.Base(value) {}
    }
}
