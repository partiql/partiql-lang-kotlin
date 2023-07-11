package org.partiql.planner

import org.partiql.ast.Identifier
import org.partiql.plan.Fn
import org.partiql.plan.Plan
import org.partiql.plan.Rex
import org.partiql.plan.Type
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.GraphType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType

// !! CATALOG PLACEHOLDER!!
class Env {

    fun resolveType(type: StaticType): Type.Ref {
        // basic type referencing
        return type.toRef()
    }

    fun resolveFn(identifier: Identifier, args: List<Rex.Op.Call.Arg>): Fn.Ref {
        return Plan.fnRefResolved(0)
    }

    // !! NEED A STATIC TYPE TO PARTIQL HEADER MAP !!
    // !! HIGHLY SIMPLIFIED FOR BOOTSTRAPPING PURPOSES !!

    private fun StaticType.toRef(): Type.Ref {
        val ordinal = when (this) {
            is AnyOfType -> 0
            is AnyType -> 0
            is NullType -> 1
            MissingType -> 1
            // Boolean types
            is BoolType -> 2
            // Numeric types
            is IntType -> 3
            is DecimalType -> 4
            is FloatType -> 5
            // Character strings
            is StringType -> 6
            is SymbolType -> 7
            // Byte strings
            is BlobType -> 8
            is ClobType -> 9
            is DateType -> TODO()
            // Collections
            is BagType -> 10
            is ListType -> 11
            is SexpType -> 12
            // Additional types
            is GraphType -> 13
            is StructType -> 14
            // Date/Time types
            is TimeType -> 15
            is TimestampType -> 16
        }
        return Plan.typeRef(ordinal)
    }
}
