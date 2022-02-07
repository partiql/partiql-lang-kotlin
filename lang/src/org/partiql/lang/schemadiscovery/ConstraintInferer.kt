package org.partiql.lang.schemadiscovery

import com.amazon.ion.IonBlob
import com.amazon.ion.IonBool
import com.amazon.ion.IonClob
import com.amazon.ion.IonDecimal
import com.amazon.ion.IonFloat
import com.amazon.ion.IonInt
import com.amazon.ion.IonList
import com.amazon.ion.IonNull
import com.amazon.ion.IonSequence
import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.ionBool
import com.amazon.ionschema.Type
import org.partiql.ionschema.model.IonSchemaModel

/**
 * Infers [IonSchemaModel.Constraint]s for a given [IonValue].
 *
 * Implementations will need to define which set of [IonType]s to infer constraints for. This will usually be for
 * all [IonType]s except DATAGRAM. In a typical use case, [inferConstraints] will be called for all scalar [IonType]s
 * and will be called recursively for all fields of an [IonStruct] and elements of an [IonSequence] (which may require
 * a [ConstraintUnifier] to return a unified [IonSchemaModel.Constraint.Element]).
 */
internal interface ConstraintInferer {
    fun inferConstraints(value: IonValue): IonSchemaModel.ConstraintList
}

/**
 * Infers [IonSchemaModel.Constraint.TypeConstraint] and additional constraints discovered using the
 * [constraintDiscoverer] for all [IonType]s except for DATAGRAM. For [IonStruct]s, also infers
 * [IonSchemaModel.Constraint.Fields] and adds [IonSchemaModel.Constraint.ClosedContent]. For [IonSequence]s, infers
 * [IonSchemaModel.Constraint.Element].
 *
 * If an [IonValue] is valid for one of the [importedTypes] (i.e. value does not violate any of the imported type's
 * constraints), then the type constraint will use the imported type's name.
 *
 * Typed null [IonValue]s will collapse to null (i.e. type: nullable::$null) and lose their type information.
 *
 * @param constraintUnifier unifies constraints for [IonSequence]s' elements
 * @param constraintDiscoverer discovers additional constraints (other than [IonSchemaModel.Constraint.TypeConstraint])
 * @param importedTypes are additional [Type]s that can be inferred for a given [IonValue].
 */
internal class TypeAndConstraintInferer(
    val constraintUnifier: ConstraintUnifier,
    val constraintDiscoverer: ConstraintDiscoverer = StandardConstraintDiscoverer(),
    private val importedTypes: List<Type> = emptyList()
): ConstraintInferer {
    private val nullNamedType = IonSchemaModel.build { namedType("\$null", nullable = ionBool(true)) }
    private val nullNamedTypeConstraintList = IonSchemaModel.build { constraintList(typeConstraint(nullNamedType)) }
    private val notNullable = ionBool(false)

    /**
     * For each [value], returns an [IonSchemaModel.ConstraintList] with the inferred type constraint and additional
     * discovered constraints using [constraintDiscoverer]. For [IonSequence]s, also infers the
     * [IonSchemaModel.Constraint.Element] constraint, unifying using [constraintUnifier] if the sequences' elements
     * have conflicting inferred constraints. For [IonStruct], infers the [IonSchemaModel.Constraint.Fields] and adds
     * the [IonSchemaModel.Constraint.ClosedContent] constraint.
     */
    override fun inferConstraints(value: IonValue): IonSchemaModel.ConstraintList {
        return when (value) {
            is IonBool -> constraintsFromScalar(value, TypeConstraint.BOOL.typeName)
            is IonInt -> constraintsFromScalar(value, TypeConstraint.INT.typeName)
            is IonFloat -> constraintsFromScalar(value, TypeConstraint.FLOAT.typeName)
            is IonDecimal -> constraintsFromScalar(value, TypeConstraint.DECIMAL.typeName)
            is IonTimestamp -> constraintsFromScalar(value, TypeConstraint.TIMESTAMP.typeName)
            is IonSymbol -> constraintsFromScalar(value, TypeConstraint.SYMBOL.typeName)
            is IonString -> constraintsFromScalar(value, TypeConstraint.STRING.typeName)
            is IonClob -> constraintsFromScalar(value, TypeConstraint.CLOB.typeName)
            is IonBlob -> constraintsFromScalar(value, TypeConstraint.BLOB.typeName)
            is IonNull -> constraintsFromScalar(value, TypeConstraint.NULL.typeName)
            is IonSexp -> constraintsFromSequence(value, TypeConstraint.SEXP.typeName)
            is IonList -> constraintsFromSequence(value, TypeConstraint.LIST.typeName)
            is IonStruct -> constraintsFromStruct(value)
            else -> error("Given $value is not supported for constraint inference")
        }
    }

    /**
     * Returns the first type name that [this] [IonValue] meets all the type constraints for among the list of
     * additional imported types. If [this] [IonValue] does not meet the type constraints for any of the additional
     * imported types, will return [typeConstraintName].
     */
    private fun IonValue.getSpecificType(typeConstraintName: String): String {
        if (this.typeAnnotations.isNotEmpty()) {
            importedTypes.forEach {
                if (it.isValid(this)) {
                    return it.name
                }
            }
        }
        return typeConstraintName
    }

    /**
     * Given a scalar type (i.e. non-sequence, non-struct), returns an [IonSchemaModel.ConstraintList] with the
     * type constraint [typeConstraintName] and additional discovered constraints. Typed nulls collapse to the null
     * type constraint.
     */
    private fun constraintsFromScalar(value: IonValue, typeConstraintName: String): IonSchemaModel.ConstraintList {
        val realTypeName = value.getSpecificType(typeConstraintName)

        if (value.isNullValue && realTypeName == typeConstraintName) {
            // null and typed nulls for scalar types collapse to null
            return nullNamedTypeConstraintList
        }

        val constraints = mutableListOf<IonSchemaModel.Constraint>(IonSchemaModel.build { typeConstraintOf(realTypeName) })
        val additionalConstraints = constraintDiscoverer.discover(value)
        constraints.addAll(additionalConstraints.items)

        return IonSchemaModel.build { constraintList(constraints) }
    }

    /**
     * Given an [IonSequence], returns an [IonSchemaModel.ConstraintList] with the sequence's type constraint,
     * additional discovered constraints, and [IonSchemaModel.Constraint.Element]. Typed nulls collapse to the null
     * type constraint.
     */
    private fun constraintsFromSequence(value: IonSequence, typeConstraintName: String): IonSchemaModel.ConstraintList {
        val sequenceTypeName = value.getSpecificType(typeConstraintName)

        if (value.isNullValue) {
            // null.list and null.sexp collapse to null
            return nullNamedTypeConstraintList
        }

        val constraints = mutableListOf<IonSchemaModel.Constraint>(IonSchemaModel.build { typeConstraintOf(sequenceTypeName) })
        val additionalConstraints = constraintDiscoverer.discover(value)
        constraints.addAll(additionalConstraints.items)

        if (value.isEmpty) {
            return IonSchemaModel.build { constraintList(constraints) }
        }

        val elementConstraintList = value.map { inferConstraints(it) }
            .asSequence()
            .unifiedConstraintList(constraintUnifier)

        if (elementConstraintList.isAny()) {
            return IonSchemaModel.build { constraintList(constraints) }
        }

        constraints.add(IonSchemaModel.build { element(type = inlineType(typeDefinition(constraints = elementConstraintList), notNullable)) })
        return IonSchemaModel.build { constraintList(constraints) }
    }

    /**
     * Given an [IonStruct], returns an [IonSchemaModel.ConstraintList] with the
     *   1. type constraint of [TypeConstraint.STRUCT] name (unless an imported type is inferred)
     *   2. [IonSchemaModel.Constraint.Fields] constraint (unless an imported type is inferred)
     *   3. additional discovered constraints (unless an imported type is inferred)
     *   4. [IonSchemaModel.Constraint.ClosedContent] (unless an imported type is inferred).
     *
     *   Typed null collapses to the null type constraint.
     */
    private fun constraintsFromStruct(value: IonStruct): IonSchemaModel.ConstraintList {
        val fields = value.associateBy(keySelector = { it.fieldName }, valueTransform = { inferConstraints(it) })
        val realTypeName = value.getSpecificType(TypeConstraint.STRUCT.typeName)

        if (realTypeName != TypeConstraint.STRUCT.typeName) {
            return IonSchemaModel.build { constraintList(typeConstraintOf(realTypeName)) }
        }

        if (value.isNullValue) {
            // null.struct collapses to null
            return nullNamedTypeConstraintList
        }

        val structConstraints = mutableListOf(
                IonSchemaModel.build { typeConstraint(namedType(TypeConstraint.STRUCT.typeName, notNullable)) },
                IonSchemaModel.build { closedContent() })

        if (fields.isNotEmpty()) {
            structConstraints.add(
                IonSchemaModel.build {
                    fields(
                        fields.map {
                            field(
                                name = it.key,
                                type = inlineType(
                                    type = typeDefinition(
                                        name = null,
                                        constraints = it.value
                                    ),
                                    nullable = notNullable
                                )
                            )
                        }
                    )
                }
            )
        }
        val additionalConstraints = constraintDiscoverer.discover(value)
        structConstraints.addAll(additionalConstraints.items)

        return IonSchemaModel.build { constraintList(structConstraints) }
    }
}
