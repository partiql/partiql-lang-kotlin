package org.partiql.lang.mockdb

import org.partiql.ionschema.model.IonSchemaModel
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.types.StaticTypeUtils
import org.partiql.types.StaticType

internal const val ISL_META_KEY = "ISL"

internal typealias TypeDefMap = Map<String, IonSchemaModel.TypeDefinition>

// FIXME: Duplicated from StaticType because of - https://github.com/partiql/partiql-lang-kotlin/issues/515
internal fun isOptional(type: StaticType) = StaticTypeUtils.getTypeDomain(type).contains(ExprValueType.MISSING)
internal fun isNullable(type: StaticType) = StaticTypeUtils.getTypeDomain(type).contains(ExprValueType.NULL)
internal fun asOptional(type: StaticType) = when {
    isOptional(type) -> type
    else -> StaticType.unionOf(type, StaticType.MISSING).flatten()
}

internal fun asNullable(type: StaticType) = when {
    isNullable(type) -> type
    else -> StaticType.unionOf(type, StaticType.NULL).flatten()
}
