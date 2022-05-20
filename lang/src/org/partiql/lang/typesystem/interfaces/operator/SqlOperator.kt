package org.partiql.lang.typesystem.interfaces.operator

/**
 * Used to define a sql operator
 */
interface SqlOperator {
    /**
     * Which operator it is
     */
    val operatorAlias: OpAlias
}
