package org.partiql.ionschema.discovery

import com.amazon.ionelement.api.ionBool
import org.partiql.ionschema.model.IonSchemaModel

/**
 * This VisitorTransform normalizes [IonSchemaModel.Constraint.AnyOf] constraints that have the null type to use
 * the `nullable` annotation. E.g.
 *
 * any_of(null, symbol, bool) -> any_of(nullable:: symbol, nullable::bool)
 * any_of(null, symbol) -> nullable::symbol
 */
class NormalizeNullableVisitorTransform : IonSchemaModel.VisitorTransform() {
    private val nullNamedType = IonSchemaModel.build { namedType("\$null", ionBool(true)) }

    /**
     * Returns whether [this] [IonSchemaModel.TypeReference] is nullable.
     */
    private fun IonSchemaModel.TypeReference.isNullable(): Boolean =
        when (this) {
            is IonSchemaModel.TypeReference.InlineType -> this.nullable.booleanValue
            is IonSchemaModel.TypeReference.NamedType -> this.nullable.booleanValue
            is IonSchemaModel.TypeReference.ImportedType -> this.nullable.booleanValue
        }

    /**
     * Returns [this] [IonSchemaModel.TypeReference]'s first [IonSchemaModel.TypeReference.NamedType] as nullable.
     */
    private fun IonSchemaModel.TypeReference.toNullable(): IonSchemaModel.TypeReference {
        val thisTypeRef = this
        return IonSchemaModel.build {
            when (thisTypeRef) {
                is IonSchemaModel.TypeReference.NamedType -> namedType(thisTypeRef.getTypename(), ionBool(true))
                is IonSchemaModel.TypeReference.InlineType ->
                    inlineType(typeDefinition(thisTypeRef.type.name?.text, thisTypeRef.type.constraints.toNullable()), ionBool(false))
                else -> error("Only InlineType and NamedType are supported")
            }
        }
    }

    /**
     * Returns [this] [IonSchemaModel.ConstraintList] with its [IonSchemaModel.Constraint.TypeConstraint] as a
     * nullable [IonSchemaModel.TypeReference].
     */
    private fun IonSchemaModel.ConstraintList.toNullable(): IonSchemaModel.ConstraintList {
        val thisTypeConstraint = this.getTypeConstraint()
        val nonTypeConstraints: List<IonSchemaModel.Constraint> = this.items.filter { it !is IonSchemaModel.Constraint.TypeConstraint }
        val nullableType = thisTypeConstraint.type.toNullable()

        val allConstraints = listOf(IonSchemaModel.build { typeConstraint(nullableType) }) + nonTypeConstraints
        return IonSchemaModel.build { constraintList(allConstraints) }
    }

    /**
     * Transforms the given [IonSchemaModel.ConstraintList], [node]. If [node] has an `any_of` constraint which
     * contains the null type, returns `any_of` with the `nullable` annotation for every other type. If [node]'s
     * `any_of` constraint has just one type T and null, returns the nullable form of T.
     */
    override fun transformConstraintList_items(node: IonSchemaModel.ConstraintList): List<IonSchemaModel.Constraint> {
        if (hasUnion(node)) {
            val anyOfTypes = node.getAnyOfConstraint().types
            if (anyOfTypes.any { it.isNullable() }) {
                val newAnyOf = anyOfTypes.filter { it != nullNamedType }
                    .map { transformTypeReference(it.toNullable()) }

                return if (newAnyOf.size == 1) {
                    newAnyOf.first().toConstraintList().items
                } else {
                    listOf(IonSchemaModel.build { anyOf(newAnyOf) })
                }
            }
            // else no null type in `any_of`
        }
        return super.transformConstraintList_items(node)
    }
}
