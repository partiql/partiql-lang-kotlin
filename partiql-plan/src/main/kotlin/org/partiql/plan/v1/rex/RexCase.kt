package org.partiql.plan.v1.rex

/**
 * Representative of the simple CASE-WHEN.
 */
public interface RexCase : Rex {

    public fun getMatch(): Rex

    public fun getBranches(): List<Branch>

    public fun getDefault(): Rex

    public interface Branch {

        public fun getCondition(): Rex

        public fun getResult(): Rex
    }
}
