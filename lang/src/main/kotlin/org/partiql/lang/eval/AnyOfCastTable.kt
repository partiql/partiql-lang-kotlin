package org.partiql.lang.eval

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.AnyType
import org.partiql.lang.types.CollectionType
import org.partiql.lang.types.SingleType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StructType

/**
 * The template table that encodes the type conversion precedence for a source type to target type.
 * This is defined in terms of the [ExprValueType] type instead of the [StaticType] because is
 * an enumeration (versus algebraic data type) and is trivially mapped to the [StaticType] instances we care
 * to associate with.
 */
private val CAST_ANY_OF_PRECEDENCE_TABLE = mapOf(
    ExprValueType.BOOL to listOf(
        ExprValueType.BOOL,
        ExprValueType.INT,
        ExprValueType.DECIMAL,
        ExprValueType.FLOAT,
        ExprValueType.STRING,
        ExprValueType.SYMBOL
    ),
    ExprValueType.INT to listOf(
        ExprValueType.INT,
        ExprValueType.DECIMAL,
        ExprValueType.FLOAT,
        ExprValueType.STRING,
        ExprValueType.SYMBOL,
        ExprValueType.BOOL
    ),
    ExprValueType.FLOAT to listOf(
        ExprValueType.FLOAT,
        ExprValueType.DECIMAL,
        ExprValueType.INT,
        ExprValueType.STRING,
        ExprValueType.SYMBOL,
        ExprValueType.BOOL
    ),
    ExprValueType.DECIMAL to listOf(
        ExprValueType.DECIMAL,
        ExprValueType.FLOAT,
        ExprValueType.INT,
        ExprValueType.STRING,
        ExprValueType.SYMBOL,
        ExprValueType.BOOL
    ),
    ExprValueType.TIMESTAMP to listOf(
        ExprValueType.STRING,
        ExprValueType.SYMBOL
        // TODO define for DATE/TIME
    ),
    ExprValueType.SYMBOL to listOf(
        ExprValueType.SYMBOL,
        ExprValueType.STRING,
        ExprValueType.DECIMAL,
        ExprValueType.INT,
        ExprValueType.FLOAT,
        ExprValueType.BOOL,
        ExprValueType.TIMESTAMP
        // TODO define for DATE/TIME/INTERVAL
    ),
    ExprValueType.STRING to listOf(
        ExprValueType.STRING,
        ExprValueType.SYMBOL,
        ExprValueType.DECIMAL,
        ExprValueType.INT,
        ExprValueType.FLOAT,
        ExprValueType.BOOL,
        ExprValueType.TIMESTAMP
        // TODO define for DATE/TIME/INTERVAL
    ),
    ExprValueType.CLOB to listOf(
        ExprValueType.CLOB,
        ExprValueType.BLOB
    ),
    ExprValueType.BLOB to listOf(
        ExprValueType.BLOB,
        ExprValueType.CLOB
    ),
    ExprValueType.LIST to listOf(
        ExprValueType.LIST,
        ExprValueType.SEXP,
        ExprValueType.BAG
    ),
    ExprValueType.SEXP to listOf(
        ExprValueType.SEXP,
        ExprValueType.LIST,
        ExprValueType.BAG
    ),
    ExprValueType.BAG to listOf(
        ExprValueType.BAG,
        ExprValueType.LIST,
        ExprValueType.SEXP
    ),
    ExprValueType.STRUCT to listOf(
        ExprValueType.STRUCT
    )
)

/** A partial compilation of the cast operation, allowing the source operand to be passed in. */
internal typealias CastFunc = (source: ExprValue) -> ExprValue

/** Represents a casted value, a failure to cast, or no possible cast target. */
private sealed class CastResult {
    /** Returns the underlying [ExprValue] or throws if in the [CastError] or [CastNil] state. */
    abstract fun unwrap(): ExprValue
}

private data class CastError(val error: EvaluationException) : CastResult() {
    override fun unwrap() = throw error
}

private data class CastValue(val value: ExprValue) : CastResult() {
    override fun unwrap() = value
}

/** Sentinel case to deal with empty target table--no compatible cast available for the source. */
private data class CastNil(val sourceType: ExprValueType, val metas: MetaContainer) : CastResult() {
    override fun unwrap(): Nothing {
        val errorContext = PropertyValueMap().also {
            it[Property.CAST_FROM] = sourceType.toString()
            // TODO put the right type name here
            it[Property.CAST_TO] = "<UNION TYPE>"
        }
        metas.sourceLocation?.let { fillErrorContext(errorContext, it) }
        err(
            "No compatible types in union to cast from $sourceType",
            ErrorCode.EVALUATOR_CAST_FAILED,
            errorContext,
            internal = false
        )
    }
}

/**
 * Represents the candidate conversion table for compiling the casting to an [AnyOfType].
 *
 * Note that currently, we cannot define recursive [StaticType] for container element types, so the
 * [CollectionType.elementType] must be `null`, but is implied to be the type given to this table.
 *
 * A further restriction is that [StructType.fields] must be empty and [StructType.contentClosed] must be
 * `false`.  It is implied similarly that the value of any `struct` fields are of the given [AnyOfType].
 *
 * @param anyOfType The union type to determine the precedence for.
 * @param metas The metadata of the compilation context.
 * @param singleTypeCast The function to delegate the implementation of a cast to a single type.
 */
internal class AnyOfCastTable(
    private val anyOfType: AnyOfType,
    private val metas: MetaContainer,
    // TODO: remove [valueFactory] once we remove [ExprValueFactory]
    private val valueFactory: ExprValueFactory,
    singleTypeCast: (SingleType) -> CastFunc
) {
    val castFuncTable: Map<ExprValueType, List<CastFunc>>
    val castTypeTable: Map<ExprValueType, List<ExprValueType>>

    init {
        val typeMap = mutableMapOf<ExprValueType, SingleType>()

        // validate the union type here
        anyOfType.types.forEach {
            when (it) {
                is AnyType -> typeErr("Union type cannot have ANY in it")
                is AnyOfType -> typeErr("Union type cannot have a Union type in it")
                is SingleType -> {
                    val runtimeType = it.runtimeType
                    if (typeMap.contains(runtimeType)) {
                        typeErr("Duplicate core type in union type not supported ($runtimeType)")
                    }
                    typeMap.put(runtimeType, it)
                    when (it) {
                        is CollectionType -> when {
                            it.elementType !is AnyType -> typeErr(
                                "Union type must have unconstrained container type (${it.elementType})"
                            )
                        }
                        is StructType -> when {
                            it.fields.isNotEmpty() -> typeErr(
                                "Union type must have no field constraints for struct (${it.fields}"
                            )
                            it.contentClosed -> typeErr("Union type must not be closed")
                        }
                        else -> {}
                    }
                }
            }
        }

        // generate the precedence table of cast target types and functions
        castTypeTable = CAST_ANY_OF_PRECEDENCE_TABLE.map { (srcType, destTypes) ->
            srcType to destTypes.filter { t -> typeMap.containsKey(t) }
        }.toMap()
        castFuncTable = castTypeTable.map { (srcType, destTypes) ->
            srcType to destTypes.mapNotNull { t -> typeMap[t] }.map(singleTypeCast)
        }.toMap()
    }

    private fun getCasts(sourceType: ExprValueType): List<CastFunc> = castFuncTable[sourceType]
        ?: throw IllegalStateException("Missing type in union cast function table: $sourceType")

    private fun firstCompatible(sourceType: ExprValueType): ExprValueType {
        val types = castTypeTable[sourceType]
            ?: throw IllegalStateException("Missing type in union cast type table: $sourceType")
        return types.firstOrNull() ?: CastNil(sourceType, metas).unwrap()
    }

    /** Evaluates the `CAST` operation over the table. */
    fun cast(source: ExprValue): ExprValue = when {
        source.isUnknown() -> source
        else -> when {
            source.type.isSequence || source.type == ExprValueType.STRUCT -> {
                // TODO honor any constraints on the container/struct type (statically)
                // sequences are a special case, we recursively cast the children into a new container
                val targetType = firstCompatible(source.type)
                val children = source.asSequence().map { cast(it) }

                when (targetType) {
                    ExprValueType.LIST -> valueFactory.newList(children)
                    ExprValueType.SEXP -> valueFactory.newSexp(children)
                    ExprValueType.BAG -> valueFactory.newBag(children)
                    ExprValueType.STRUCT -> {
                        if (source.type != ExprValueType.STRUCT) {
                            // Should not be possible
                            throw IllegalStateException("Cannot cast from non-struct to struct")
                        }
                        valueFactory.newStruct(
                            children.zip(source.asSequence()).map { (child, original) ->
                                child.namedValue(original.name!!)
                            },
                            StructOrdering.UNORDERED
                        )
                    }
                    else -> throw IllegalStateException("Invalid collection target type: $targetType")
                }
            } else -> {
                // for the scalar case, we apply the available cast functions in order
                // and either we succeed with a converted value, or we get an error and keep trying
                var result: CastResult = CastNil(source.type, metas)
                loop@ for (castFunc in getCasts(source.type)) {
                    when (result) {
                        is CastNil, is CastError -> {
                            try {
                                result = CastValue(castFunc(source))
                            } catch (e: EvaluationException) {
                                result = CastError(e)
                            }
                        }
                        is CastValue -> break@loop
                    }
                }
                result.unwrap()
            }
        }
    }

    private fun typeErr(message: String): Nothing = err(
        message,
        ErrorCode.SEMANTIC_UNION_TYPE_INVALID,
        errorContextFrom(metas),
        internal = true
    )
}
