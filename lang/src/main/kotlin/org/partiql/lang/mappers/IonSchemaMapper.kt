package org.partiql.lang.mappers

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import org.partiql.ionschema.model.IonSchemaModel
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.AnyType
import org.partiql.lang.types.BagType
import org.partiql.lang.types.BlobType
import org.partiql.lang.types.BoolType
import org.partiql.lang.types.ClobType
import org.partiql.lang.types.DateType
import org.partiql.lang.types.DecimalType
import org.partiql.lang.types.FloatType
import org.partiql.lang.types.IntType
import org.partiql.lang.types.ListType
import org.partiql.lang.types.MissingType
import org.partiql.lang.types.NullType
import org.partiql.lang.types.NumberConstraint
import org.partiql.lang.types.SchemaType
import org.partiql.lang.types.SexpType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StringType
import org.partiql.lang.types.StructType
import org.partiql.lang.types.SymbolType
import org.partiql.lang.types.TimeType
import org.partiql.lang.types.TimestampType

internal const val ISL_META_KEY = "ISL"

/**
 * This class transforms a StaticType into an ISL schema with one or more top-level type definitions.
 *
 * The decision of whether we create a top-level type or an inline type depends on the presence of "name" attribute within ISL present in the `metas` field for a StaticType
 * This is based on the requirement that we only allow top-level ISL types to have a `name` and enforce inline ISL types to not have a `name` attribute
 */
class IonSchemaMapper(private val staticType: StaticType) {

    private val topLevelTypeLookup = staticType.visit(mapOf())

    /**
     * Builds an Ion Schema from a StaticType. The resulting Ion Schema may have one or more top-level type definitions
     * There will always be a top-level type created with name as [typeName]
     * Ion Schema should always import "partiql.isl" by default to import partiql defined types such as bag, missing, etc.
     */
    fun toIonSchema(topLevelTypeName: String): IonSchemaModel.Schema {
        val remaining = topLevelTypeLookup.filterNot { it.key == topLevelTypeName }
        return IonSchemaModel.build {
            schema(
                // header
                listOf(headerStatement(openFieldList(), importList(import("partiql.isl")))) +
                    // other top-level type statements
                    remaining.mapValues { typeStatement(it.value) }.values.toList() +
                    // type statement for `topLevelTypeName`
                    typeStatement(staticType.toTypeDefinition(topLevelTypeName, typeDefName = topLevelTypeName)) +
                    // footer
                    footerStatement(openFieldList())
            )
        }
    }

    /**
     * Creates a top-level or an inline ISL type definition
     */
    private fun StaticType.toTypeDefinition(topLevelTypeName: String, typeDefName: String? = null, constraints: Set<IonSchemaModel.Constraint> = emptySet()): IonSchemaModel.TypeDefinition =
        IonSchemaModel.build {
            typeDefinition(
                typeDefName,
                constraints = when {
                    constraints.isEmpty() -> constraintList(getConstraints(topLevelTypeName, typeDefName).toList())
                    else -> constraintList(constraints.toList())
                }
            )
        }

    /**
     * Returns a set of all constraints for the StaticType
     */
    private fun StaticType.getConstraints(topLevelTypeName: String, typeDefName: String? = null): Set<IonSchemaModel.Constraint> =
        setOf(getTypeConstraint(topLevelTypeName, typeDefName)) + getOtherConstraints(topLevelTypeName, typeDefName)

    /**
     * This method creates either a type constraint or an any_of constraint
     * If type constraint is a top-level type, create type constraint with top-level type name as its value
     * If any_of constraint contains a top-level type, create any_of constraint containing that top-level type name
     *
     * @param typeDefName Name of top-level type definition that type constraint is being created for.
     *
     * Examples: // [] indicates union type
     *
     * |----------------------------------------------------------------------------------------------------|
     * | StaticType                      | ISL                                                              |
     * |----------------------------------------------------------------------------------------------------|
     * | STRING                          | type: string                                                     |
     * | [STRING]                        | type: string                                                     |
     * | [NULL, INT]                     | type: nullable::int                                              |
     * | [NULL, INT, STRING]             | any_of: [nullable::int, nullable::string]                        |
     * | [NULL, INT, ListType(STRING)]   | any_of: [nullable::int, nullable::{type: list, element: string}] |
     * | [INT, [NULL, STRING]]           | any_of: [nullable::int, nullable::string]                        |
     * | [[NULL, INT], [NULL, STRING]]   | any_of: [nullable::int, nullable::string]                        |
     * | [INT, STRING, ListType(STRING)] | any_of: [int, string, {type: list, element: string}]             |
     * |----------------------------------------------------------------------------------------------------|
     */
    private fun StaticType.getTypeConstraint(topLevelTypeName: String, typeDefName: String? = null, nullable: Boolean = false): IonSchemaModel.Constraint = when (this) {
        is AnyOfType -> this.handleAnyOfConstraint(topLevelTypeName)
        else -> {
            // Examples: type:int, type:{type:list,element:int}, type:custom
            // We only create type constraint here
            val isNullable = ionBool(isNullable(this) || nullable)

            // Get type definitions stored in metas
            @Suppress("UNCHECKED_CAST")
            val typeDefsFromMetas = metas[ISL_META_KEY] as? List<IonSchemaModel.TypeDefinition>
            // Get type definition for `typeDefName` if exists. Note that `typeDefName` may be null but there may still be a valid type definition.
            val currentTypeDef = typeDefsFromMetas?.firstOrNull { typeDefName == it.name?.text }
            if (currentTypeDef == null) {
                // No type definition exists in metas for `typeDefName`; use base type name
                IonSchemaModel.build { typeConstraint(namedType(getBaseTypeName(), isNullable)) }
            } else {
                // Type definition exists
                when (val name = currentTypeDef.name?.text) {
                    null -> {
                        // name is null; use top-level name reference if type constraint is a top-level type, otherwise create type constraint using base type name
                        val typeConstraintName = currentTypeDef.getTypeConstraintName()
                        when {
                            typeConstraintName != null && typeConstraintName.isNotTypeDefName(typeDefName) && isTopLevelType(typeConstraintName, topLevelTypeName) -> {
                                IonSchemaModel.build { typeConstraint(namedType(typeConstraintName, isNullable)) }
                            }
                            else -> {
                                // type constraint is not present or is not a top-level type
                                // create type constraint using base type name (e.g. "int" for IntType, and so on)
                                IonSchemaModel.build { typeConstraint(namedType(getBaseTypeName(), isNullable)) }
                            }
                        }
                    }
                    else -> {
                        // name is present in ISL within metas
                        when {
                            name.isNotTypeDefName(typeDefName) && isTopLevelType(name, topLevelTypeName) -> {
                                IonSchemaModel.build { typeConstraint(namedType(name, isNullable)) }
                            }
                            else -> {
                                // name is `typeDefName` or is not top-level type
                                // create type constraint using base type name (e.g. "int" for IntType, and so on)
                                IonSchemaModel.build { typeConstraint(namedType(getBaseTypeName(), isNullable)) }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Utility method to flatten out [IonSchemaModel.TypeReference].
     *
     * Examples:
     *
     *   inlineType({type: string, codepoint_length: 5})) -> inlineType({type: string, codepoint_length: 5})
     *   inlineType({type: string}) -> namedType(string)
     *   inlineType({type: {type: string}}) -> namedType(string)
     *   inlineType({type: {type: string, codepoint_length: 5}}) -> inlineType({type: string, codepoint_length: 5})
     *   inlineType({type: nullable::string}) -> namedType(string, nullable = true)
     *   inlineType({type: string}, nullable = true) -> namedType(string, nullable = true)
     */
    private fun IonSchemaModel.TypeReference.flatten(): IonSchemaModel.TypeReference = when (this) {
        is IonSchemaModel.TypeReference.NamedType -> this
        is IonSchemaModel.TypeReference.InlineType -> when (this.type.constraints.items.size) {
            1 -> when (val constraint = this.type.constraints.items.first()) {
                is IonSchemaModel.Constraint.TypeConstraint -> when (val typeFromConstraint = constraint.type) {
                    is IonSchemaModel.TypeReference.NamedType -> IonSchemaModel.build {
                        namedType(
                            typeFromConstraint.name.text,
                            ionBool(this@flatten.nullable.booleanValue || typeFromConstraint.nullable.booleanValue)
                        )
                    }
                    is IonSchemaModel.TypeReference.InlineType -> IonSchemaModel.build {
                        inlineType(
                            typeFromConstraint.type,
                            ionBool(this@flatten.nullable.booleanValue || typeFromConstraint.nullable.booleanValue)
                        ).flatten()
                    }
                    is IonSchemaModel.TypeReference.ImportedType -> TODO("Imports are not supported yet")
                }
                else -> this
            }
            else -> this
        }
        is IonSchemaModel.TypeReference.ImportedType -> TODO("Imports are not supported yet")
    }

    /**
     * Creates any_of constraint (or type constraint, if AnyOfType is a union of a single StaticType, possibly with Null type)
     */
    private fun AnyOfType.handleAnyOfConstraint(topLevelTypeName: String): IonSchemaModel.Constraint =
        when (val flattenedType = flatten()) {
            is AnyOfType -> {
                val nullable = isNullable(flattenedType)
                val nonNullableTypes = flattenedType.types.filterNot { isNullable(it) }
                when (nonNullableTypes.size) {
                    0 -> {
                        error("Should not happen since flattenedType is an AnyOfType and therefore must be a union of at least two types")
                    }
                    1 -> {
                        IonSchemaModel.build {
                            typeConstraint(nonNullableTypes[0].toTypeReference(topLevelTypeName, nullable))
                        }
                    }
                    else -> {
                        // Examples: any_of:[int, string], any_of:[int, custom], any_of:[custom1, custom2]
                        IonSchemaModel.build {
                            anyOf(
                                nonNullableTypes.map {
                                    it.toTypeReference(topLevelTypeName, nullable)
                                }
                            )
                        }
                    }
                }
            }
            else -> {
                IonSchemaModel.build {
                    typeConstraint(flattenedType.toTypeReference(topLevelTypeName, nullable = false))
                }
            }
        }

    /**
     * Add constraints from StaticType definition and also propagate constraints from ISL in metas (that may not have been used for inference)
     *
     * Constraints in StaticType may map to ISL that is different from the ISL inside metas.
     * To ensure that we don't add the same constraint again with different ISL, we apply the following rule:
     * If a constraint can been added from StaticType, then we ignore instances of the same constraint that may be present inside ISL in metas
     *
     * @param topLevelTypeName top-level type name passed by caller to the mapper
     * @param typeDefName name of the ISL type definition for the StaticType
     */
    private fun StaticType.getOtherConstraints(topLevelTypeName: String, typeDefName: String? = null): Set<IonSchemaModel.Constraint> {
        // Get type definitions from metas, if present
        @Suppress("UNCHECKED_CAST")
        val typeDefsFromMetas = metas[ISL_META_KEY] as? List<IonSchemaModel.TypeDefinition> ?: listOf()

        // If there are multiple type definitions, only consider constraints for the relevant one
        // The type def we are interested in has name as `typeDefName` (which may be null, for instance, if we are getting constraints for a struct field)
        // Once the correct type def is identified, get all constraints excluding
        // 1. type/any_of constraint since we handle those separately
        // 2. occurs constraint because it should be reasoned about and added based on field in [StructType] being optional or not,
        //   but not based on what it was in the originating ISL (i.e in the metas)
        var constraintsFromISL = typeDefsFromMetas.firstOrNull { typeDefName == it.name?.text }?.constraints?.items?.filterNot {
            it is IonSchemaModel.Constraint.TypeConstraint || it is IonSchemaModel.Constraint.AnyOf || it is IonSchemaModel.Constraint.Occurs
        } ?: listOf()

        return when (this) {
            is StringType -> listOfNotNull(
                when (val lengthConstraint = this.lengthConstraint) {
                    StringType.StringLengthConstraint.Unconstrained -> null
                    is StringType.StringLengthConstraint.Constrained -> {
                        constraintsFromISL = constraintsFromISL.filterNot { it is IonSchemaModel.Constraint.CodepointLength }
                        when (lengthConstraint.length) {
                            is NumberConstraint.Equals -> IonSchemaModel.build {
                                codepointLength(equalsNumber(ionInt(lengthConstraint.length.value.toLong())))
                            }
                            is NumberConstraint.UpTo -> IonSchemaModel.build {
                                codepointLength(
                                    equalsRange(
                                        numberRange(
                                            inclusive(ionInt(0)),
                                            inclusive(ionInt(lengthConstraint.length.value.toLong()))
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            )
            is IntType -> {
                when (val constraint = this.rangeConstraint) {
                    IntType.IntRangeConstraint.UNCONSTRAINED -> emptyList()
                    else -> {
                        constraintsFromISL = constraintsFromISL.filterNot { it is IonSchemaModel.Constraint.ValidValues }
                        listOf(
                            IonSchemaModel.build {
                                validValues(
                                    rangeOfValidValues(
                                        numRange(
                                            numberRange(
                                                inclusive(ionInt(constraint.validRange.first)),
                                                inclusive(ionInt(constraint.validRange.last))
                                            )
                                        )
                                    )
                                )
                            }
                        )
                    }
                }
            }
            is DecimalType -> when (val precisionScaleConstraint = this.precisionScaleConstraint) {
                DecimalType.PrecisionScaleConstraint.Unconstrained -> listOf()
                is DecimalType.PrecisionScaleConstraint.Constrained -> {
                    constraintsFromISL = constraintsFromISL.filterNot {
                        it is IonSchemaModel.Constraint.Precision || it is IonSchemaModel.Constraint.Scale
                    }
                    val precision = precisionScaleConstraint.precision
                    listOf(
                        // StaticType's decimal precision represents an inclusive "upto" range. Maps to an exact
                        // precision if value is 1. Otherwise (for positive values) maps to inclusive range of
                        // 1 to value.
                        when {
                            precision < 1 -> error("Precision must be a positive integer")
                            precision == 1 -> IonSchemaModel.build { precision(equalsNumber(ionInt(1))) }
                            else -> IonSchemaModel.build { precision(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(precision.toLong()))))) }
                        },
                        IonSchemaModel.build { scale(equalsNumber(ionInt(precisionScaleConstraint.scale.toLong()))) }
                    )
                }
            }
            is StructType -> listOfNotNull(
                if (contentClosed) {
                    constraintsFromISL = constraintsFromISL.filterNot { it is IonSchemaModel.Constraint.ClosedContent }
                    IonSchemaModel.build { closedContent() }
                } else null,

                if (fields.isNotEmpty()) {
                    constraintsFromISL = constraintsFromISL.filterNot { it is IonSchemaModel.Constraint.Fields }
                    IonSchemaModel.build { fields(fields.toFieldList(topLevelTypeName)) }
                } else null
            )
            is ListType -> listOfNotNull(
                if (elementType != StaticType.ANY) {
                    constraintsFromISL = constraintsFromISL.filterNot { it is IonSchemaModel.Constraint.Element }
                    IonSchemaModel.build { element(elementType.toTypeReference(topLevelTypeName = topLevelTypeName)) }
                } else null
            )
            is SexpType -> listOfNotNull(
                if (elementType != StaticType.ANY) {
                    constraintsFromISL = constraintsFromISL.filterNot { it is IonSchemaModel.Constraint.Element }
                    IonSchemaModel.build { element(elementType.toTypeReference(topLevelTypeName = topLevelTypeName)) }
                } else null
            )
            is BagType -> listOfNotNull(
                if (elementType != StaticType.ANY) {
                    constraintsFromISL = constraintsFromISL.filterNot { it is IonSchemaModel.Constraint.Element }
                    IonSchemaModel.build { element(elementType.toTypeReference(topLevelTypeName = topLevelTypeName)) }
                } else null
            )
            is AnyOfType -> {
                // Ignore metas from union types
                constraintsFromISL = listOf()
                listOf()
            }
            else -> constraintsFromISL /* No constraints are supported for other PartiQL types yet */
        }.toSet() + constraintsFromISL.toSet()
    }

    /**
     * StaticType may be mapped in ISL as either a named type or an inline type
     *
     * A Named type is created if StaticType has ISL in metas with a top-level type name
     * An Inline type is created in all other cases for simplicity
     */
    private fun StaticType.toTypeReference(topLevelTypeName: String, nullable: Boolean = false): IonSchemaModel.TypeReference {
        // If ISL in metas has exactly one top-level type, create type reference as a named type with that top-level type name
        // In all other cases, create an inline type definition
        @Suppress("UNCHECKED_CAST")
        val typeDefsFromMetas = metas[ISL_META_KEY] as? List<IonSchemaModel.TypeDefinition> ?: listOf()
        if (typeDefsFromMetas.size == 1) {
            val typeDefName = typeDefsFromMetas[0].name?.text
            if (typeDefName != null) {
                return when {
                    isTopLevelType(typeDefName, topLevelTypeName) -> IonSchemaModel.build { namedType(typeDefName, ionBool(isNullable(this@toTypeReference) || nullable)) }
                    else -> throw TypeNotFoundException(typeDefName) // Should not happen since type def should already have been accumulated as a top-level type in [StaticType.visit]
                }
            }
        }

        return IonSchemaModel.build { inlineType(toTypeDefinition(topLevelTypeName), ionBool(nullable)) }.flatten()
    }

    private fun Map<String, StaticType>.toFieldList(topLevelTypeName: String): List<IonSchemaModel.Field> =
        mapNotNull {
            it.toFieldOrNull(topLevelTypeName)
        }

    private fun Map.Entry<String, StaticType>.toFieldOrNull(topLevelTypeName: String): IonSchemaModel.Field? =
        if (value is MissingType || (value is AnyOfType && (value as AnyOfType).flatten() is MissingType)) {
            // Skip field altogether
            null
        } else {
            IonSchemaModel.build {
                field(
                    name = key,
                    type = when {
                        isOptional(value) -> {
                            // Field is either AnyType or a union of Missing and other type(s)
                            when (val staticType = value) {
                                is AnyType -> {
                                    staticType.toTypeReference(topLevelTypeName)
                                }
                                is AnyOfType -> {
                                    when (val flattened = staticType.flatten()) {
                                        is AnyType -> {
                                            flattened.toTypeReference(topLevelTypeName)
                                        }
                                        is AnyOfType -> {
                                            val nonMissingTypes = flattened.types.filterNot { it is MissingType }
                                            val typeWithoutMissing = StaticType.unionOf(nonMissingTypes.toSet(), staticType.metas)
                                            typeWithoutMissing.toTypeReference(topLevelTypeName)
                                        }
                                        else -> {
                                            error("Optional struct field must either be Any or a union of Missing and other type(s)")
                                        }
                                    }
                                }
                                else -> {
                                    error("Optional struct field is neither Any or a union type")
                                }
                            }
                        }
                        else -> {
                            when (val reference = value.toTypeReference(topLevelTypeName)) {
                                is IonSchemaModel.TypeReference.NamedType -> inlineType(
                                    typeDefinition(
                                        null,
                                        constraintList(typeConstraint(reference), occurs(occursRequired()))
                                    ),
                                    reference.nullable
                                )
                                is IonSchemaModel.TypeReference.InlineType -> inlineType(
                                    typeDefinition(
                                        null,
                                        constraintList(reference.type.constraints.items + occurs(occursRequired()))
                                    ),
                                    reference.nullable
                                )
                                is IonSchemaModel.TypeReference.ImportedType -> TODO("Imported types are not supported yet")
                            }
                        }
                    }
                )
            }
        }

    /**
     * This method checks if the name of top-level type is the same as type constraint name
     * This method is used to prevent us from incorrectly creating type constraint as `typeDefName`
     * We don't want the value of type constraint to be the same as its type definition name
     *
     * @param typeDefName name of top-level type definition
     */
    private fun String.isNotTypeDefName(typeDefName: String?) = this != typeDefName

    private fun isTopLevelType(name: String, topLevelTypeName: String) =
        when {
            topLevelTypeLookup.filterNot { it.key == topLevelTypeName }.containsKey(name) -> true
            else -> false
        }
}

private fun IonSchemaModel.TypeDefinition.getTypeConstraint() = constraints.items.firstOrNull { it is IonSchemaModel.Constraint.TypeConstraint }

/**
 * If type constraint is present, return its value
 * For named types: type constraint value is a string symbol
 * For inline types: type constraint is an inlined type definition and cannot have a "name" (by virtue of being inline)
 *
 * This method is called to get the type constraint name (if present) in order to check if it is a top-level ISL type definition
 */
private fun IonSchemaModel.TypeDefinition.getTypeConstraintName(): String? {
    val typeConstraint = getTypeConstraint() ?: return null
    return when (val typeRef = (typeConstraint as IonSchemaModel.Constraint.TypeConstraint).type) {
        is IonSchemaModel.TypeReference.NamedType -> typeRef.name.text
        is IonSchemaModel.TypeReference.InlineType -> null
        is IonSchemaModel.TypeReference.ImportedType -> TODO("Imports are not supported yet")
    }
}

/**
 * A top-level type is found when ISL type definition in metas has "name" attribute present - this is only possible if
 * it was a top-level type in original ISL (ISL used to create the StaticType instance)
 */
private fun StaticType.addTopLevelTypesFromMetas(): Map<String, IonSchemaModel.TypeDefinition> {
    @Suppress("UNCHECKED_CAST")
    val typeDefs = this.metas[ISL_META_KEY] as? List<IonSchemaModel.TypeDefinition> ?: emptyList()
    return typeDefs.filter { it.name != null }.map { it.name!!.text to it }.toMap()
}

/**
 * Traverses StaticType and accumulates all top-level ISL type definitions
 */
private fun StaticType.visit(accumulator: TypeDefMap): TypeDefMap {
    var current = accumulator + this.addTopLevelTypesFromMetas()
    when (this) {
        is ListType -> {
            current = this.elementType.visit(current)
        }
        is SexpType -> {
            current = this.elementType.visit(current)
        }
        is BagType -> {
            current = this.elementType.visit(current)
        }
        is StructType -> {
            this.fields.mapValues { current = it.value.visit(current) } // visit fields
        }
        is AnyOfType -> {
            when (val flattenedType = flatten()) {
                is AnyOfType -> {
                    // visit types
                    flattenedType.types.map { current = it.visit(current) }
                }
                else -> current = current + flattenedType.visit(current)
            }
        }
    }
    return current
}

/**
 * Convenience method to get the name of an ISL core type corresponding to given StaticType
 * Note that "missing" and "bag" are not valid ISL 1.0 core types but are added here for completeness
 */
fun StaticType.getBaseTypeName(): String = when (this) {
    is IntType -> "int"
    is FloatType -> "float"
    is DecimalType -> "decimal"
    is AnyType -> "any"
    is ListType -> "list"
    is SexpType -> "sexp"
    is BagType -> "bag"
    is NullType -> "\$null"
    MissingType -> "missing"
    is BoolType -> "bool"
    is TimestampType -> "timestamp"
    is SymbolType -> "symbol"
    is StringType -> "string"
    is BlobType -> "blob"
    is ClobType -> "clob"
    is StructType -> "struct"
    is AnyOfType -> {
        when (val flattenedType = flatten()) {
            is AnyOfType -> {
                val nonNullableTypes = flattenedType.types.filterNot { isNullable(it) }
                when (nonNullableTypes.size) {
                    0 -> "\$null"
                    1 -> nonNullableTypes.first().getBaseTypeName()
                    else -> "any_of" // Only used for serialization of StaticTypeMeta corresponding to an AnyOfType
                }
            }
            else -> flattenedType.getBaseTypeName()
        }
    }
    is DateType -> "date"
    is TimeType -> when (withTimeZone) {
        false -> "time"
        true -> "time_with_time_zone"
    }
    is SchemaType -> "schema"
}
