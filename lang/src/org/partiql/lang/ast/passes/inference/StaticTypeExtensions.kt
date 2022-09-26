package org.partiql.lang.ast.passes.inference

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.Failed
import org.partiql.lang.ots.interfaces.Plugin
import org.partiql.lang.ots.interfaces.Successful
import org.partiql.lang.ots.interfaces.TypeInferenceResult
import org.partiql.lang.ots.interfaces.Uncertain
import org.partiql.lang.ots.plugins.standard.types.BlobType
import org.partiql.lang.ots.plugins.standard.types.CharType
import org.partiql.lang.ots.plugins.standard.types.ClobType
import org.partiql.lang.ots.plugins.standard.types.DecimalType
import org.partiql.lang.ots.plugins.standard.types.FloatType
import org.partiql.lang.ots.plugins.standard.types.Int2Type
import org.partiql.lang.ots.plugins.standard.types.Int4Type
import org.partiql.lang.ots.plugins.standard.types.Int8Type
import org.partiql.lang.ots.plugins.standard.types.IntType
import org.partiql.lang.ots.plugins.standard.types.StringType
import org.partiql.lang.ots.plugins.standard.types.SymbolType
import org.partiql.lang.ots.plugins.standard.types.VarcharType
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.AnyType
import org.partiql.lang.types.CollectionType
import org.partiql.lang.types.MissingType
import org.partiql.lang.types.NullType
import org.partiql.lang.types.SingleType
import org.partiql.lang.types.StaticScalarType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StructType
import org.partiql.lang.util.ots_work.ScalarOpId
import org.partiql.lang.util.ots_work.inferReturnType

internal fun StaticType.isNullOrMissing(): Boolean = (this is NullType || this is MissingType)
internal fun StaticType.isText(): Boolean = this is StaticScalarType && (scalarType in listOf(SymbolType, StringType, VarcharType, CharType))
internal fun StaticType.isNumeric(): Boolean = this is StaticScalarType && (scalarType in listOf(Int2Type, Int4Type, Int8Type, IntType, FloatType, DecimalType))
internal fun StaticType.isLob(): Boolean = this is StaticScalarType && (scalarType === BlobType || scalarType === ClobType)
internal fun StaticType.isUnknown(): Boolean = (this.isNullOrMissing() || this == StaticType.NULL_OR_MISSING)

/**
 * Casts [this] static to the given target type.
 *
 * This replicates the behavior of its runtime equivalent [ExprValue.cast].
 * @see [ExprValue.cast] for documentation.
 */
internal fun StaticType.cast(targetType: StaticType, plugin: Plugin): StaticType {
    when (targetType) {
        is AnyOfType -> {
            // TODO we should do more sophisticated inference based on the source like we do for single types
            val includesNull = this.allTypes.any { it.isNullable() }
            return when {
                includesNull -> StaticType.unionOf(StaticType.MISSING, StaticType.NULL, targetType)
                else -> StaticType.unionOf(StaticType.MISSING, targetType)
            }
        }
        is AnyType -> {
            // casting to `ANY` is the identity
            return this
        }
        is SingleType -> {}
    }

    // union source types, recursively process them
    when (this) {
        is AnyType -> return AnyOfType(this.toAnyOfType().types.map { it.cast(targetType, plugin) }.toSet()).flatten()
        is AnyOfType -> return when (val flattened = this.flatten()) {
            is SingleType, is AnyType -> flattened.cast(targetType, plugin)
            is AnyOfType -> AnyOfType(flattened.types.map { it.cast(targetType, plugin) }.toSet()).flatten()
        }
    }

    // single source type
    return when {
        this.isNullOrMissing() && targetType == StaticType.MISSING -> StaticType.MISSING
        this.isNullOrMissing() && targetType == StaticType.NULL -> StaticType.NULL
        // `MISSING` and `NULL` always convert to themselves no matter the target type
        this.isNullOrMissing() -> this
        else -> when {
            targetType is StaticScalarType && this is StaticScalarType -> {
                val inferResult = plugin.inferReturnType(
                    ScalarOpId.ScalarCast,
                    toCompileTimeType(),
                    targetType.toCompileTimeType()
                )
                inferResult.toStaticType()
            }
            targetType is CollectionType && this is CollectionType -> targetType
            targetType is StructType && this is StructType -> targetType
            else -> StaticType.MISSING // TODO:  support non-permissive mode(s) here by throwing an exception to indicate cast is not possible
        }
    }
}

/**
 * For [this] [StaticType], filters out [NullType] and [MissingType] from [AnyOfType]s. Otherwise, returns [this].
 */
internal fun StaticType.filterNullMissing(): StaticType =
    when (this) {
        is AnyOfType -> AnyOfType(this.types.filter { !it.isNullOrMissing() }.toSet()).flatten()
        else -> this
    }

/**
 * Returns a human-readable string of [argTypes]. Additionally, for each [AnyOfType], [NullType] and [MissingType] will
 * be filtered.
 */
internal fun stringWithoutNullMissing(argTypes: List<StaticType>): String =
    argTypes.joinToString { it.filterNullMissing().toString() }

internal fun CompileTimeType.toSingleType() = StaticScalarType(scalarType, parameters)

internal fun TypeInferenceResult.toStaticType() =
    when (this) {
        Failed -> StaticType.MISSING
        is Successful -> compileTimeType.toSingleType()
        is Uncertain -> StaticType.unionOf(StaticType.MISSING, compileTimeType.toSingleType())
    }

internal fun Set<SingleType>.toStaticType() = when (size) {
    0 -> StaticType.MISSING
    1 -> first()
    else -> StaticType.unionOf(this)
}

internal fun List<CompileTimeType>.toStaticType() = map { it.toSingleType() }.toSet().toStaticType()
