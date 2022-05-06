package org.partiql.lang.typesystem.interfaces.operator

import org.partiql.lang.typesystem.interfaces.type.Type

/**
 * Used to define a sql operator
 */
interface PqlOperator {
    /**
     * Which operator it is
     */
    val operatorAlias: OpAlias

    /**
     * Type assigned to the return value
     */
    val returnType: Type
}
