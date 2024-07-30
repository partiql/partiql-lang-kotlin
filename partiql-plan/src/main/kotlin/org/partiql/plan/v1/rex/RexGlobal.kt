package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
interface RexGlobal : Rex {

    fun getCatalog(): String

    /**
     * TODO replace with Catalog Name
     */
    fun getName(): String
}
