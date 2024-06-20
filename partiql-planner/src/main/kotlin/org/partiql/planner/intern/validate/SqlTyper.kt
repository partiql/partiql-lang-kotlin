package org.partiql.planner.intern.validate

import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.CollectionType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.SexpType
import org.partiql.types.CompilerType
import org.partiql.types.CompilerType.Companion.MISSING
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint

/**
 * Single place for computing type.
 *
 * Notes,
 * - I actually prefer raising an interface, but not necessary now.
 * - There is a lot of potential on how to split typing judgements so we don't end up with the giant mess of PlanTyper.
 * - Some consistency might be nice on how this is done?
 */
internal object SqlTyper {

    /**
     * TODO hardcoded for now; shouldn't be needed with PType.
     */
    private val factory = CompilerTypes

    /**
     * Compute a FROM Clause return type.
     */
    fun getScanType(type: CompilerType): CompilerType = when (type) {
        is BagType -> type.elementType
        is ListType -> type.elementType
        is AnyType -> factory.dynamic()
        is AnyOfType -> factory.dynamic(type.types.map { getScanType(it) })
        else -> type
    }

    /**
     * Compute an UNPIVOT Clause return type.
     */
    fun getUnpivotType(type: CompilerType): CompilerType {
        val variants = type.allTypes.map { variant ->
            if (variant !is StructType) {
                return variant
            }
            return when {
                variant.isClosed() -> factory.dynamic(variant.fields.map { it.value })
                variant.fields.isEmpty() -> factory.struct()
                else -> factory.dynamic()
            }
        }
        return factory.dynamic(variants)
    }

    /**
     * Compute the return type for a Rex.Op.Path.Index
     */
    fun getPathIndexType(rootT: CompilerType, keyT: CompilerType): CompilerType {
        val elementTypes = rootT.allTypes.map { type ->
            if (type !is ListType && type !is SexpType) {
                return@map CompilerType.MISSING
            }
            (type as CollectionType).elementType
        }.toSet()
        if (elementTypes.all { it is MissingType }) {
            return CompilerType.MISSING
        }
        return factory.dynamic(elementTypes)
    }

    /**
     * Compute the return type for a Rex.Op.Path.Key
     */
    fun getPathKeyType(rootT: CompilerType, keyT: CompilerType): CompilerType {
        TODO()
    }

    // -- Helpers

    /**
     * This isn't 100%, but is logic extracted from unpivot value
     */
    private fun StructType.isClosed(): Boolean {
        return contentClosed || constraints.contains(TupleConstraint.Open(false))
    }
}
