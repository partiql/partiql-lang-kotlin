package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
public interface RexTupleUnion : Rex {

    public fun getArgs(): List<Rex>
}
