package org.partiql.planner.internal.casts

import org.partiql.planner.internal.ir.Ref.Cast
import org.partiql.planner.internal.ir.refCast
import org.partiql.value.ArrayType
import org.partiql.value.BagType
import org.partiql.value.BoolType
import org.partiql.value.CharType
import org.partiql.value.CharVarUnboundedType
import org.partiql.value.ClobType
import org.partiql.value.DynamicType
import org.partiql.value.Float32Type
import org.partiql.value.Float64Type
import org.partiql.value.Int16Type
import org.partiql.value.Int32Type
import org.partiql.value.Int64Type
import org.partiql.value.Int8Type
import org.partiql.value.MissingType
import org.partiql.value.NullType
import org.partiql.value.NumericType
import org.partiql.value.PartiQLCoreTypeBase
import org.partiql.value.PartiQLType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BAG
import org.partiql.value.PartiQLValueType.BINARY
import org.partiql.value.PartiQLValueType.BLOB
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.BYTE
import org.partiql.value.PartiQLValueType.CHAR
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.DECIMAL
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.PartiQLValueType.INTERVAL
import org.partiql.value.PartiQLValueType.LIST
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.PartiQLValueType.NULL
import org.partiql.value.PartiQLValueType.SEXP
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.STRUCT
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP
import org.partiql.value.TupleType

/**
 * A place to model type relationships (for now this is to answer CAST inquiries).
 *
 * @property types
 * @property graph      Going with a matrix here (using enum ordinals) as it's simple and avoids walking.
 */
@OptIn(PartiQLValueExperimental::class)
internal class CastTable private constructor(
    private val types: Array<PartiQLType>,
    private val graph: Array<Array<Cast?>>,
) {

    fun get(operand: PartiQLType, target: PartiQLType): Cast? {
        val safety = when (target) {
            is NumericType -> {
                val targetPrecision = target.precision ?: (NumericType.MAX_PRECISION + 1)
                val targetScale = target.scale ?: (NumericType.MAX_SCALE + 1)
                when (operand) {
                    is NumericType -> {
                        val valuePrecision = operand.precision ?: (NumericType.MAX_PRECISION + 1)
                        val valueScale = operand.scale ?: (NumericType.MAX_SCALE + 1)
                        when {
                            targetPrecision < valuePrecision -> Cast.Safety.UNSAFE
                            targetScale < valueScale -> Cast.Safety.UNSAFE
                            else -> Cast.Safety.COERCION
                        }
                    }
                    is Int32Type -> {
                        if (targetPrecision < Int32Type.PRECISION) Cast.Safety.UNSAFE else Cast.Safety.COERCION // TODO: Handle decimals
                    }
                    is Int64Type -> {
                        if (targetPrecision < Int64Type.PRECISION) Cast.Safety.UNSAFE else Cast.Safety.COERCION // TODO: Handle decimals
                    }
                    DynamicType -> getOld(operand, target)?.safety // TODO
                    MissingType -> getOld(operand, target)?.safety // TODO
                    is PartiQLCoreTypeBase -> getOld(operand, target)?.safety // TODO
                    is PartiQLType.Runtime.Custom -> getOld(operand, target)?.safety // TODO
                }
            }
            is Int8Type -> when (operand) {
                is NumericType -> {
                    val valuePrecision = operand.precision ?: (NumericType.MAX_PRECISION + 1)
                    val valueScale = operand.scale ?: (NumericType.MAX_SCALE + 1)
                    if (valueScale != 0 || valuePrecision >= Int8Type.PRECISION) Cast.Safety.UNSAFE else Cast.Safety.COERCION
                }
                else -> getOld(operand, target)?.safety // TODO
            }
            is Int16Type -> when (operand) {
                is NumericType -> {
                    val valuePrecision = operand.precision ?: (NumericType.MAX_PRECISION + 1)
                    val valueScale = operand.scale ?: (NumericType.MAX_SCALE + 1)
                    if (valueScale != 0 || valuePrecision >= Int16Type.PRECISION) Cast.Safety.UNSAFE else Cast.Safety.COERCION
                }
                else -> getOld(operand, target)?.safety // TODO
            }
            is Int32Type -> when (operand) {
                is NumericType -> {
                    val valuePrecision = operand.precision ?: (NumericType.MAX_PRECISION + 1)
                    val valueScale = operand.scale ?: (NumericType.MAX_SCALE + 1)
                    if (valueScale != 0 || valuePrecision >= Int32Type.PRECISION) Cast.Safety.UNSAFE else Cast.Safety.COERCION
                }
                else -> getOld(operand, target)?.safety // TODO
            }
            is Int64Type -> when (operand) {
                is NumericType -> {
                    val valuePrecision = operand.precision ?: (NumericType.MAX_PRECISION + 1)
                    val valueScale = operand.scale ?: (NumericType.MAX_SCALE + 1)
                    if (valueScale != 0 || valuePrecision >= Int64Type.PRECISION) Cast.Safety.UNSAFE else Cast.Safety.COERCION
                }
                else -> getOld(operand, target)?.safety // TODO
            }
            is Float32Type -> when (operand) {
                is NumericType -> Cast.Safety.UNSAFE // TODO: Is this correct?
                else -> getOld(operand, target)?.safety // TODO
            }
            is Float64Type -> when (operand) {
                is NumericType -> when (operand.precision == null) {
                    true -> Cast.Safety.UNSAFE
                    false -> Cast.Safety.UNSAFE // TODO: Is this correct? This MIGHT be safe.
                }
                else -> getOld(operand, target)?.safety // TODO
            }
            DynamicType -> getOld(operand, target)?.safety // TODO
            MissingType -> getOld(operand, target)?.safety // TODO
            is PartiQLCoreTypeBase -> getOld(operand, target)?.safety // TODO
            is PartiQLType.Runtime.Custom -> getOld(operand, target)?.safety // TODO
        }
        return when (safety) {
            null -> null
            else -> Cast(operand, target, safety)
        }
    }

    private fun getOld(operand: PartiQLType, target: PartiQLType): Cast? {
        val i = types.indexOf(operand)
        val j = types.indexOf(target)
        if (i == -1 || j == -1) {
            return null
            // TODO: Use this for checking errors:
            // error("Could not find CAST for value $operand ($i) to target $target ($j).")
        }
        return graph[i][j]
    }

    /**
     * Returns the CAST function if exists, else null.
     * TODO: Should we allow UNSAFE coercions? According to SQL, we can coerce NUMERIC(5, 1) to NUMERIC(3, 1)
     */
    fun lookupCoercion(operand: PartiQLType, target: PartiQLType): Cast? {
        val cast = get(operand, target) ?: return null
        return if (cast.safety == Cast.Safety.COERCION) cast else null
    }

    private operator fun <T> Array<T>.get(t: PartiQLValueType): T = get(t.ordinal)

    companion object {

        private val N = PartiQLValueType.values().size

        private operator fun <T> Array<T>.set(t: PartiQLValueType, value: T): Unit = this.set(t.ordinal, value)

        private fun PartiQLType.relationships(types: Array<PartiQLType>, block: RelationshipBuilder.() -> Unit): Array<Cast?> {
            return with(RelationshipBuilder(this, types)) {
                block()
                build()
            }
        }

        /**
         * Build the PartiQL type lattice.
         *
         * TODO this is incomplete.
         */
        @JvmStatic
        val partiql: CastTable = run {
            val types = PartiQLValueType.values().mapIndexed { idx, pType -> idx to PartiQLType.fromLegacy(pType) }
            val soleTypes = types.map { it.second }.toTypedArray()
            val graph = arrayOfNulls<Array<Cast?>>(N)
            for (type in types) {
                // initialize all with empty relationships
                graph[type.first] = arrayOfNulls(N)
            }
            graph[ANY] = DynamicType.relationships(soleTypes) {
                coercion(DynamicType)
                types.filterNot { it.second is DynamicType }.forEach {
                    unsafe(it.second)
                }
            }
            graph[NULL] = NullType.relationships(soleTypes) {
                coercion(NullType)
            }
            graph[MISSING] = MissingType.relationships(soleTypes) {
                coercion(MissingType)
            }
            graph[BOOL] = BoolType.relationships(soleTypes) {
                coercion(BoolType)
                explicit(Int8Type)
                explicit(Int16Type)
                explicit(Int32Type)
                explicit(Int64Type)
                explicit(NumericType(null, 0)) // TODO: Int. Go through all the possible values
                explicit(NumericType(null, null)) // TODO: Unbounded
                explicit(Float32Type)
                explicit(Float64Type)
                explicit(CharType(1)) // TODO
                explicit(CharVarUnboundedType)
                explicit(CharVarUnboundedType)
            }
            graph[INT8] = Int8Type.relationships(soleTypes) {
                explicit(BoolType)
                coercion(Int8Type)
                coercion(Int16Type)
                coercion(Int32Type)
                coercion(Int64Type)
                coercion(NumericType(null, null))
                // coercion(INT)
                // explicit(NUMERIC) // TODO: How to handle?
                // coercion(NUMERIC_ARBITRARY)
                coercion(Float32Type)
                coercion(Float64Type)
                explicit(CharVarUnboundedType)
                explicit(CharVarUnboundedType)
            }
            graph[INT16] = Int16Type.relationships(soleTypes) {
                explicit(BoolType)
                unsafe(Int8Type)
                coercion(Int16Type)
                coercion(Int32Type)
                coercion(Int64Type)
                coercion(NumericType(null, null))
                // coercion(INT)
                // explicit(NUMERIC) // TODO: How to handle?
                // coercion(NUMERIC_ARBITRARY)
                coercion(Float32Type)
                coercion(Float64Type)
                explicit(CharVarUnboundedType)
                explicit(CharVarUnboundedType)
            }
            graph[INT32] = Int32Type.relationships(soleTypes) {
                explicit(BoolType)
                unsafe(Int8Type)
                unsafe(Int16Type)
                coercion(Int32Type)
                coercion(Int64Type)
                coercion(NumericType(null, null))
                explicit(NumericType(null, null)) // TODO: How to handle
                // coercion(INT)
                // explicit(NUMERIC) // TODO: How to handle?
                // coercion(NUMERIC_ARBITRARY)
                coercion(Float32Type)
                coercion(Float64Type)
                explicit(CharVarUnboundedType)
                explicit(CharVarUnboundedType)
            }
            graph[INT64] = Int64Type.relationships(soleTypes) {
                explicit(BoolType)
                unsafe(Int8Type)
                unsafe(Int16Type)
                unsafe(Int32Type)
                coercion(Int64Type)
                coercion(NumericType(null, null))
                // coercion(INT)
                // explicit(NUMERIC) // TODO: How to handle?
                // coercion(NUMERIC_ARBITRARY)
                coercion(Float32Type)
                coercion(Float64Type)
                explicit(CharVarUnboundedType)
                explicit(CharVarUnboundedType)
            }
            graph[INT] = NumericType(null, null).relationships(soleTypes) {
                explicit(BoolType)
                unsafe(Int8Type)
                unsafe(Int16Type)
                unsafe(Int32Type)
                unsafe(Int64Type)
                coercion(NumericType(null, null))
                // coercion(INT)
                // explicit(NUMERIC) // TODO: How to handle?
                // coercion(NUMERIC_ARBITRARY)
                coercion(Float32Type)
                coercion(Float64Type)
                explicit(CharVarUnboundedType)
                explicit(CharVarUnboundedType)
            }
            graph[DECIMAL] = NumericType(null, null).relationships(soleTypes) {
                explicit(BoolType)
                unsafe(Int8Type)
                unsafe(Int16Type)
                unsafe(Int32Type)
                unsafe(Int64Type)
                unsafe(NumericType(null, null))
                // unsafe(INT)
                // coercion(NUMERIC) // TODO: How to handle?
                // coercion(NUMERIC_ARBITRARY)
                explicit(Float32Type)
                explicit(Float64Type)
                explicit(CharVarUnboundedType)
                explicit(CharVarUnboundedType)
            }
            graph[DECIMAL_ARBITRARY] = NumericType(null, null).relationships(soleTypes) {
                explicit(BoolType)
                unsafe(Int8Type)
                unsafe(Int16Type)
                unsafe(Int32Type)
                unsafe(Int64Type)
                unsafe(NumericType(null, null))
                // unsafe(INT)
                // coercion(NUMERIC) // TODO: How to handle?
                // coercion(NUMERIC_ARBITRARY)
                explicit(Float32Type)
                explicit(Float64Type)
                explicit(CharVarUnboundedType)
                explicit(CharVarUnboundedType)
            }
            graph[FLOAT32] = Float32Type.relationships(soleTypes) {
                explicit(BoolType)
                unsafe(Int8Type)
                unsafe(Int16Type)
                unsafe(Int32Type)
                unsafe(Int64Type)
                unsafe(NumericType(null, null))
                // unsafe(INT)
                // coercion(NUMERIC) // TODO: How to handle?
                // coercion(NUMERIC_ARBITRARY)
                coercion(Float32Type)
                coercion(Float64Type)
                explicit(CharVarUnboundedType)
                explicit(CharVarUnboundedType)
            }
            graph[FLOAT64] = Float64Type.relationships(soleTypes) {
                explicit(BoolType)
                unsafe(Int8Type)
                unsafe(Int16Type)
                unsafe(Int32Type)
                unsafe(Int64Type)
                unsafe(NumericType(null, null))
                // unsafe(INT)
                // unsafe(NUMERIC) // TODO: How to handle?
                // coercion(NUMERIC_ARBITRARY)
                unsafe(Float32Type)
                coercion(Float64Type)
                explicit(CharVarUnboundedType)
                explicit(CharVarUnboundedType)
            }
            graph[CHAR] = CharType(1).relationships(soleTypes) {
                explicit(BoolType)
                coercion(CharType(1)) // TODO: Length
                coercion(CharVarUnboundedType)
                coercion(CharVarUnboundedType)
            }
            graph[STRING] = CharVarUnboundedType.relationships(soleTypes) {
                explicit(BoolType)
                unsafe(Int8Type)
                unsafe(Int16Type)
                unsafe(Int32Type)
                unsafe(Int64Type)
                unsafe(NumericType(null, null))
                coercion(CharVarUnboundedType)
                explicit(CharVarUnboundedType)
                coercion(ClobType(10)) // TODO: Length
            }
            graph[SYMBOL] = CharVarUnboundedType.relationships(soleTypes) {
                explicit(BoolType)
                coercion(CharVarUnboundedType)
                coercion(CharVarUnboundedType)
                coercion(ClobType(10))
            }
            graph[CLOB] = ClobType(10).relationships(soleTypes) {
                coercion(ClobType(10)) // TODO: Handle
            }
            graph[BINARY] = arrayOfNulls(N)
            graph[BYTE] = arrayOfNulls(N)
            graph[BLOB] = arrayOfNulls(N)
            graph[DATE] = arrayOfNulls(N)
            graph[TIME] = arrayOfNulls(N)
            graph[TIMESTAMP] = arrayOfNulls(N)
            graph[INTERVAL] = arrayOfNulls(N)
            graph[BAG] = BagType.relationships(soleTypes) {
                coercion(BagType)
            }
            graph[LIST] = ArrayType.relationships(soleTypes) {
                coercion(BagType)
                coercion(ArrayType)
                coercion(ArrayType)
            }
            graph[SEXP] = ArrayType.relationships(soleTypes) {
                coercion(BagType)
                coercion(ArrayType)
                coercion(ArrayType)
            }
            graph[STRUCT] = TupleType.relationships(soleTypes) {
                coercion(TupleType)
            }
            CastTable(soleTypes, graph.requireNoNulls())
        }
    }

    private class RelationshipBuilder(val operand: PartiQLType, val types: Array<PartiQLType>) {

        private val relationships = arrayOfNulls<Cast?>(N)

        fun build() = relationships

        fun coercion(target: PartiQLType) {
            val i = types.indexOfFirst { it.javaClass == target.javaClass } // TODO: val i = types.indexOf(target)
            relationships[i] = refCast(operand, target, Cast.Safety.COERCION)
        }

        fun explicit(target: PartiQLType) {
            val i = types.indexOfFirst { it.javaClass == target.javaClass } // TODO: val i = types.indexOf(target)
            relationships[i] = refCast(operand, target, Cast.Safety.EXPLICIT)
        }

        fun unsafe(target: PartiQLType) {
            val i = types.indexOfFirst { it.javaClass == target.javaClass } // TODO: val i = types.indexOf(target)
            relationships[i] = refCast(operand, target, Cast.Safety.UNSAFE)
        }
    }
}
