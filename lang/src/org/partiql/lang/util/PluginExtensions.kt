package org.partiql.lang.util

import OTS.ITF.org.partiql.ots.Plugin
import OTS.ITF.org.partiql.ots.type.ScalarType

internal class TypeRegistry(types: List<ScalarType>) {
    private var namesToType: Map<String, ScalarType> = types.flatMap { scalarType ->
        scalarType.names.map { typeAlias -> typeAlias to scalarType }
    }.associate { it.first to it.second }

    fun getTypeByName(name: String): ScalarType? = namesToType[name]
}

/**
 * Used by PartiQL compile pipeline to validate a plugin when taking it at build time
 */
internal fun Plugin.validate() {
    // Validate scalar types
    val typeNames = mutableSetOf<String>()
    val typeAliases = mutableSetOf<String>()
    scalarTypes.forEach { type ->
        // Type name checking
        if (typeNames.contains(type.id)) {
            error("Type name '$typeNames' are registered twice")
        }
        typeNames.add(type.id)

        // Type alias checking
        type.names.forEach { alias ->
            // TODO: Check type alias not to collide with PartiQL keywords except type-related keywords
            if (typeAliases.contains(alias)) {
                error("Type alias '$alias' are registered twice")
            }
            typeAliases.add(alias)
        }
    }
}
