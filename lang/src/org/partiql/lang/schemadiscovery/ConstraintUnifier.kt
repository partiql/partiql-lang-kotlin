package org.partiql.lang.schemadiscovery

import com.amazon.ionelement.api.ionBool
import org.partiql.ionschema.model.IonSchemaModel

private val notNullable = ionBool(false)

/**
 * Defines the strategy to unify two constraint lists with different [IonSchemaModel.Constraint.TypeConstraint]s.
 *
 * When [UNION], conflicting type constraints will be included in a union (i.e. [IonSchemaModel.Constraint.AnyOf].
 *   e.g.    type: int
 *     with  type: decimal
 *       ->  any_of(int, decimal)
 *
 * When [ANY], conflicting type constraints will result in ANY (i.e. no type constraint)
 *   e.g.    type: int
 *     with  type: decimal
 *       ->  any
 */
internal enum class ConflictStrategy {
    UNION,
    ANY
}

/**
 * Defines the behavior when unifying two different struct constraint lists.
 *
 * When [INTERSECTION], only the [IonSchemaModel.Field]s that are in both structs are included in the output
 *   e.g.   { a: int, b: string }
 *     with { a: int, c: decimal }
 *       -> { a: int }
 *
 * When [UNION], all the [IonSchemaModel.Field]s that are in the structs are included in the output. Any fields that
 * have different constraint lists are unified according to the passed ConstraintUnifier
 *   e.g.   { a: int, b: string }
 *     with { a: int, c: decimal }
 *       -> { a: int, b: string, c: decimal }
 *
 *   e.g.   { a: int }
 *     with { a: decimal }
 *       -> { a: unify(int, decimal) }
 *
 * (Tentative definition)
 * When [INTERSECTION_AS_REQUIRED], all the [IonSchemaModel.Field]s that are in the structs are included in the output,
 * but any fields that appear in both are marked as [IonSchemaModel.Optionality.Required].
 */
internal enum class StructBehavior {
    INTERSECTION {
        override fun unifyStructs(
            unifier: ConstraintUnifier,
            structA: IonSchemaModel.ConstraintList,
            structB: IonSchemaModel.ConstraintList
        ): IonSchemaModel.ConstraintList {
            TODO("Not yet implemented")
        }
    },
    UNION {
        override fun unifyStructs(
            unifier: ConstraintUnifier,
            structA: IonSchemaModel.ConstraintList,
            structB: IonSchemaModel.ConstraintList
        ): IonSchemaModel.ConstraintList {
            if (structA.isEmptyStruct()) {
                return structB.addClosedContentConstraint()
            } else if (structB.isEmptyStruct()) {
                return structA.addClosedContentConstraint()
            }

            val aFields = structA.getFieldsConstraint().fields.toSet()
            val bFields = structB.getFieldsConstraint()
            val unifiedFields = aFields.toMutableSet()

            bFields.fields.forEach { bField ->
                val aField = aFields.find { it.name == bField.name }
                if (aField != null) {
                    if (aField != bField) {
                        val unifiedField = IonSchemaModel.build {
                            field_(aField.name, inlineType(typeDefinition(null, unifier.unify(aField.type.toConstraintList(), bField.type.toConstraintList())), notNullable))
                        }
                        unifiedFields.remove(aField)
                        unifiedFields.add(unifiedField)
                    }
                    // Otherwise, has same name and value type
                } else {
                    // `structA` doesn't have `structB`'s field
                    unifiedFields.add(bField)
                }
            }
            return IonSchemaModel.build {
                constraintList(structA.getTypeConstraint(), closedContent(), fields(unifiedFields.toList()))
            }
        }
    },
    INTERSECTION_AS_REQUIRED {
        override fun unifyStructs(
            unifier: ConstraintUnifier,
            structA: IonSchemaModel.ConstraintList,
            structB: IonSchemaModel.ConstraintList
        ): IonSchemaModel.ConstraintList {
            TODO("Not yet implemented")
        }
    };

    abstract fun unifyStructs(
        unifier: ConstraintUnifier,
        structA: IonSchemaModel.ConstraintList,
        structB: IonSchemaModel.ConstraintList
    ): IonSchemaModel.ConstraintList
}

/**
 * Interface for unifying two [IonSchemaModel.ConstraintList]s. A [ConstraintUnifier] is intended to be used by
 * [SchemaInferencerFromExample] when either
 *   1. unifying the types and/or other constraints of a sequence of examples or
 *   2. unifying the [IonSchemaModel.Constraint.Element] type for sequence types.
 */
internal interface ConstraintUnifier {
    fun unify(aConstraints: IonSchemaModel.ConstraintList, bConstraints: IonSchemaModel.ConstraintList): IonSchemaModel.ConstraintList

    companion object {
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var sequenceTypes = listOf(TypeConstraint.SEXP.typeName, TypeConstraint.LIST.typeName)
        private var conflictStrategy = ConflictStrategy.UNION
        private var structBehavior = StructBehavior.UNION
        private var discoveredConstraintUnifier: DiscoveredConstraintUnifier = MultipleTypedDCU()

        /**
         * Defines the sequence type names (i.e. SEXP, LIST, and other imported types). Defaults to
         * [TypeConstraint.SEXP] and [TypeConstraint.LIST].
         */
        fun sequenceTypes(types: List<String>): Builder = this.apply { sequenceTypes = types }

        /**
         * Defines the type conflict unification strategy. Defaults to [ConflictStrategy.UNION].
         */
        fun conflictStrategy(strategy: ConflictStrategy): Builder = this.apply { conflictStrategy = strategy }

        /**
         * Defines the struct field unification behavior. Defaults to [StructBehavior.UNION].
         */
        fun structBehavior(behavior: StructBehavior): Builder = this.apply { structBehavior = behavior }

        /**
         * Defines how additional discovered constraints are to be unified. Defaults to use [MultipleTypedDCU] (which
         * defaults to using the [standardTypedDiscoveredConstraintUnifiers]).
         */
        fun discoveredConstraintUnifier(unifier: DiscoveredConstraintUnifier): Builder = this.apply { discoveredConstraintUnifier = unifier }

        fun build(): ConstraintUnifier {
            return ConstraintUnifierImpl(sequenceTypes, conflictStrategy, structBehavior, discoveredConstraintUnifier)
        }
    }
}

private class ConstraintUnifierImpl(
    private val sequenceTypes: List<String>,
    val conflictStrategy: ConflictStrategy,
    val structBehavior: StructBehavior,
    val discoveredConstraintUnifier: DiscoveredConstraintUnifier
) : ConstraintUnifier {
    /**
     * Unifies [aConstraints] with [bConstraints].
     */
    override fun unify(aConstraints: IonSchemaModel.ConstraintList, bConstraints: IonSchemaModel.ConstraintList): IonSchemaModel.ConstraintList {
        if (aConstraints == bConstraints) {
            return aConstraints
        }

        when (conflictStrategy) {
            ConflictStrategy.UNION -> when {
                hasUnion(aConstraints) && hasUnion(bConstraints) -> {
                    // `aConstraints` and `bConstraints` are unions
                    val bTypes = bConstraints.getAnyOfConstraint().types.map { it.toConstraintList() }
                    return bTypes.fold(aConstraints) { union, bConstraintList ->
                        unifyUnionWithNonUnion(union = union.getAnyOfConstraint(), nonUnion = bConstraintList)
                    }
                }
                hasUnion(aConstraints) -> {
                    // only `aConstraints` is a union
                    return unifyUnionWithNonUnion(union = aConstraints.getAnyOfConstraint(), nonUnion = bConstraints)
                }
                hasUnion(bConstraints) -> {
                    // only `bConstraints` is a union
                    return unifyUnionWithNonUnion(union = bConstraints.getAnyOfConstraint(), nonUnion = aConstraints)
                }
                else -> {
                    // `aConstraints` and `bConstraints` are not unions
                    val aTypeName = aConstraints.getTypeConstraint().type.getTypename()
                    val bTypeName = bConstraints.getTypeConstraint().type.getTypename()

                    return if (aTypeName == bTypeName) {
                        IonSchemaModel.build { unifyNonUnionTypes(aConstraints, bConstraints) }
                    } else {
                        // typenames of `aConstraints` and `bConstraints` are different so create union
                        IonSchemaModel.build {
                            constraintList(anyOf(aConstraints.toTypeReference(), bConstraints.toTypeReference()))
                        }
                    }
                }
            }
            ConflictStrategy.ANY -> TODO("Not yet implemented")
        }
    }

    /**
     * Unifies [sequenceA] sequence constraint list with [sequenceB].
     *
     * In this implementation, on constraint conflict of sequence elements, the [IonSchemaModel.Constraint.Element]
     * will be unified using [unify]. Thus, the resulting unification can result in heterogeneous elements in the
     * sequence (e.g. type: list, element: { any_of { int, decimal }).
     *
     * TODO: decide if this should be a configurable
     */
    private fun unifySequences(sequenceA: IonSchemaModel.ConstraintList, sequenceB: IonSchemaModel.ConstraintList): IonSchemaModel.ConstraintList {
        // items.size == 1 -> no element type so is empty sequence
        if (sequenceA.items.size == 1) {
            return sequenceB
        } else if (sequenceB.items.size == 1) {
            return sequenceA
        }

        // Type is either a sexp, list, or other sequence type
        val aTypeName = sequenceA.getTypeConstraint().type.getTypename()

        val aElement = sequenceA.getElementConstraint().type as IonSchemaModel.TypeReference.InlineType
        val bElement = sequenceB.getElementConstraint().type as IonSchemaModel.TypeReference.InlineType

        val elementTypeConstraints = unify(aElement.type.constraints, bElement.type.constraints)
        if (elementTypeConstraints.isAny()) {
            return IonSchemaModel.build {
                constraintList(typeConstraint(namedType(aTypeName, notNullable)))
            }
        }

        return IonSchemaModel.build {
            constraintList(
                typeConstraint(namedType(aTypeName, notNullable)),
                element(inlineType(typeDefinition(name = null, constraints = elementTypeConstraints), notNullable))
            )
        }
    }

    /**
     * Unifies [union] (union of types), with [nonUnion] (a single type). If [nonUnion]'s type is not in [union], then
     * adds [nonUnion] to [union]. Otherwise, unify [nonUnion] with it's corresponding type in [union].
     */
    private fun unifyUnionWithNonUnion(union: IonSchemaModel.Constraint.AnyOf, nonUnion: IonSchemaModel.ConstraintList): IonSchemaModel.ConstraintList {
        val anyOfConstraintList = union.types.toMutableList()

        val nonUnionType = nonUnion.getTypeConstraint().type
        val nonUnionTypeName = nonUnionType.getTypename()

        val matchingTypeIndex = anyOfConstraintList.indexOfFirst {
            it.getTypename() == nonUnionTypeName
        }

        when (matchingTypeIndex) {
            -1 -> {
                // no matching typename, so append to union
                anyOfConstraintList.add(nonUnion.toTypeReference())
            }
            else -> {
                // there's a matching typename
                val matchingType = anyOfConstraintList[matchingTypeIndex]
                val matchingTypeConstraints = matchingType.toConstraintList()

                if (matchingTypeConstraints != nonUnion) {
                    // typenames equal but need to resolve other constraint conflicts
                    anyOfConstraintList[matchingTypeIndex] = unifyNonUnionTypes(matchingTypeConstraints, nonUnion).toTypeReference()
                }
                // else constraint already in union
            }
        }
        return IonSchemaModel.build { constraintList(anyOf(anyOfConstraintList)) }
    }

    /**
     * Unifies [a] and [b] non-union types. Requires
     *   1. [a] != [b]
     *   2. [a] and [b] have the same typename
     */
    private fun unifyNonUnionTypes(a: IonSchemaModel.ConstraintList, b: IonSchemaModel.ConstraintList): IonSchemaModel.ConstraintList {
        val constraints = when {
            a.isScalarType() && b.isScalarType() -> listOf(a.getTypeConstraint())
            a.isStructType() && b.isStructType() -> structBehavior.unifyStructs(this@ConstraintUnifierImpl, a, b).items
            a.isSequenceType() && b.isSequenceType() -> unifySequences(a, b).items
            else -> error("$a and $b typenames do not match")
        }
        val discoveredConstraints = discoveredConstraintUnifier(a, b).items
        return IonSchemaModel.build { constraintList(constraints + discoveredConstraints) }
    }

    /**
     * Returns true if and only if [this] [IonSchemaModel.ConstraintList]'s type constraint is a sequence type.
     * Sequence types include `list`, `sexp`, and any imported sequence types.
     */
    private fun IonSchemaModel.ConstraintList.isSequenceType(): Boolean {
        val constraintType = this.getTypeConstraint()
        val name = constraintType.type.getTypename()
        return sequenceTypes.contains(name)
    }

    /**
     * Returns true if and only if [this] [IonSchemaModel.ConstraintList]'s type constraint is struct.
     */
    private fun IonSchemaModel.ConstraintList.isStructType(): Boolean {
        val constraintType = this.getTypeConstraint()
        val name = constraintType.type.getTypename()
        return name == TypeConstraint.STRUCT.typeName
    }

    /**
     * Returns true if and only if [this] [IonSchemaModel.ConstraintList]'s type constraint is not one of sexp, list,
     * defined sequence types, or struct.
     */
    private fun IonSchemaModel.ConstraintList.isScalarType(): Boolean {
        return !this.isSequenceType() && !this.isStructType()
    }
}
