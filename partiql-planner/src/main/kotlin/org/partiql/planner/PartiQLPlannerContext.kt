package org.partiql.planner

import org.partiql.plan.Fn
import org.partiql.plan.Global
import org.partiql.plan.Identifier
import org.partiql.plan.PartiQLHeader
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
import org.partiql.types.PartiQLValueType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType

/**
 * Hardcoded PartiQL Global Catalog
 *
 * !! ONLY DOING ATOMIC TYPES FOR V0 !!
 * !! NEED TO BACK BY A CATALOG!!
 * !! HIGHLY SIMPLIFIED FOR BOOTSTRAPPING PURPOSES !!
 */
internal class PartiQLPlannerContext(private val session: PartiQLPlanner.Session) {

    private val factory = Plan
    private val catalog = Catalog.partiql()

    private val globals = mutableListOf<Global>()

    // TEMPORARY
    internal fun header(): PartiQLHeader = factory.partiQLHeader(
        types = catalog.types,
        functions = catalog.functions,
    )

    // TEMPORARY
    internal fun globals(): List<Global> = globals

    // TYPES

    /**
     * Get a Type.Ref from a Type.Atomic
     */
    internal fun resolveType(type: Type.Atomic): Type.Ref {
        catalog.types.forEachIndexed { i, t ->
            // need .equals() if we want to include more variants
            if (t.symbol == type.symbol) {
                return Plan.typeRef(t.symbol, i)
            }
        }
        throw IllegalArgumentException("Catalog does not contain type ${type.symbol}")
    }

    /**
     * Get a Type.Ref from a StaticType
     */
    internal fun resolveType(type: StaticType): Type.Ref {
        val symbol = when (type) {
            is AnyOfType -> "any"
            is AnyType -> "any"
            is BlobType -> "blob"
            is BoolType -> "bool"
            is ClobType -> "clob"
            is BagType -> "bag"
            is ListType -> "list"
            is SexpType -> "sexp"
            is DateType -> "date"
            is DecimalType -> "decimal"
            is FloatType -> "float"
            is GraphType -> "graph"
            is IntType -> "int"
            MissingType -> "missing"
            is NullType -> "null"
            is StringType -> "string"
            is StructType -> "struct"
            is SymbolType -> "symbol"
            is TimeType -> "time"
            is TimestampType -> "timestamp"
        }
        val t =  Plan.typeAtomic(symbol)
        return resolveType(t)
    }

    internal fun resolveType(type: PartiQLValueType): Type.Ref {
        val symbol = when (type) {
            PartiQLValueType.BOOL -> "bool"
            PartiQLValueType.INT8 -> "int8"
            PartiQLValueType.INT16 -> "int16"
            PartiQLValueType.INT32 -> "int32"
            PartiQLValueType.INT64 -> "int64"
            PartiQLValueType.INT -> "int"
            PartiQLValueType.DECIMAL -> "decimal"
            PartiQLValueType.FLOAT32 -> "float32"
            PartiQLValueType.FLOAT64 -> "float64"
            PartiQLValueType.CHAR -> "char"
            PartiQLValueType.STRING -> "string"
            PartiQLValueType.SYMBOL -> "symbol"
            PartiQLValueType.BINARY -> "binary"
            PartiQLValueType.BYTE -> "byte"
            PartiQLValueType.BLOB -> "blob"
            PartiQLValueType.CLOB -> "clob"
            PartiQLValueType.DATE -> "date"
            PartiQLValueType.TIME -> "time"
            PartiQLValueType.TIMESTAMP -> "timestamp"
            PartiQLValueType.INTERVAL -> "interval"
            PartiQLValueType.BAG -> "bag"
            PartiQLValueType.LIST -> "list"
            PartiQLValueType.SEXP -> "sexp"
            PartiQLValueType.STRUCT -> "struct"
            PartiQLValueType.NULL -> "null"
            PartiQLValueType.MISSING -> "missing"
            PartiQLValueType.NULLABLE_BOOL -> "bool"
            PartiQLValueType.NULLABLE_INT8 -> "int8"
            PartiQLValueType.NULLABLE_INT16 -> "int16"
            PartiQLValueType.NULLABLE_INT32 -> "int32"
            PartiQLValueType.NULLABLE_INT64 -> "int64"
            PartiQLValueType.NULLABLE_INT -> "int"
            PartiQLValueType.NULLABLE_DECIMAL -> "decimal"
            PartiQLValueType.NULLABLE_FLOAT32 -> "float32"
            PartiQLValueType.NULLABLE_FLOAT64 -> "float64"
            PartiQLValueType.NULLABLE_CHAR -> "char"
            PartiQLValueType.NULLABLE_STRING -> "string"
            PartiQLValueType.NULLABLE_SYMBOL -> "symbol"
            PartiQLValueType.NULLABLE_BINARY -> "binary"
            PartiQLValueType.NULLABLE_BYTE -> "byte"
            PartiQLValueType.NULLABLE_BLOB -> "blob"
            PartiQLValueType.NULLABLE_CLOB -> "clob"
            PartiQLValueType.NULLABLE_DATE -> "date"
            PartiQLValueType.NULLABLE_TIME -> "time"
            PartiQLValueType.NULLABLE_TIMESTAMP -> "timestamp"
            PartiQLValueType.NULLABLE_INTERVAL -> "interval"
            PartiQLValueType.NULLABLE_BAG -> "bag"
            PartiQLValueType.NULLABLE_LIST -> "list"
            PartiQLValueType.NULLABLE_SEXP -> "sexp"
            PartiQLValueType.NULLABLE_STRUCT -> "struct"
        }
        val t =  Plan.typeAtomic(symbol)
        return resolveType(t)
    }

    // FUNCTIONS

    /**
     * This will need to be greatly improved upon. We will need to return some kind of pair which has a list of
     * implicit casts to introduce.
     */
    internal fun resolveFn(identifier: Identifier, args: List<Rex.Op.Call.Arg>): Fn.Ref {
        when (identifier) {
            is Identifier.Qualified -> throw IllegalArgumentException("Qualified function identifiers not supported")
            is Identifier.Symbol -> {
                val symbol = identifier.symbol.lowercase()
                // TODO actual function resolution
                return Plan.fnRefResolved(symbol, 0)
            }
        }
    }

    internal fun resolveAggFn(identifier: Identifier, args: List<Rex.Op.Call.Arg>): Fn.Ref {
        when (identifier) {
            is Identifier.Qualified -> throw IllegalArgumentException("Qualified function identifiers not supported")
            is Identifier.Symbol -> {
                val symbol = identifier.symbol.lowercase()
                // TODO actual function resolution
                return Plan.fnRefResolved(symbol, 0)
            }
        }
    }
    internal fun resolveGlobal(identifier: Identifier): Rex.Op.Global? {
        // TODO
        return null
    }
}
