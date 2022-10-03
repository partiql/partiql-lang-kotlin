package org.partiql.lang.util

import OTS.ITF.org.partiql.ots.Plugin

internal fun Plugin.mapAliasToScalarType() = scalarTypes.flatMap { scalarType ->
    scalarType.aliases.map { typeAlias -> typeAlias to scalarType }
}.associate { it.first to it.second }

/**
 * Used by PartiQL compile pipeline to validate a plugin when taking it at build time
 */
internal fun Plugin.validate() {
    // Validate scalar types
    val typeNames = mutableSetOf<String>()
    val typeAliases = mutableSetOf<String>()
    scalarTypes.forEach { type ->
        // Type name checking
        if (typeNames.contains(type.typeName)) {
            error("Type name '$typeNames' are registered twice")
        }
        typeNames.add(type.typeName)

        // Type alias checking
        type.aliases.forEach { alias ->
            if (alias.contains(' ')) {
                error("Type alias '$alias' has a space")
            }
            // TODO: Check type alias not to collide with PartiQL keywords except type-related keywords
            if (typeAliases.contains(alias)) {
                error("Type alias '$alias' are registered twice")
            }
            typeAliases.add(alias)
        }
    }
}
