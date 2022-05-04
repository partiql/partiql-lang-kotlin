package org.partiql.lang.typesystem.interfaces.operator

/**
 * Used to define a sql operator
 */
interface PqlOperator {
    /**
     * Which operator it is
     */
    fun getOperatorAlias(): OpAlias
}