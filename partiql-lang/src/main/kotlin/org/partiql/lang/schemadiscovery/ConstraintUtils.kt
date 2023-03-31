package org.partiql.lang.schemadiscovery

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import org.partiql.ionschema.model.IonSchemaModel
import kotlin.math.max
import kotlin.math.min

internal val emptyConstraintList = IonSchemaModel.build { constraintList() }

private fun IonSchemaModel.ConstraintList.getConstraint(constraint: Class<out IonSchemaModel.Constraint>): IonSchemaModel.Constraint =
    this.items.find { it.javaClass == constraint }
        ?: throw IllegalStateException("Given constraint list $this does not have any $constraint")

/**
 * Returns the first [IonSchemaModel.Constraint.TypeConstraint] in [this] constraint list. Throws an
 * [IllegalStateException] if [this] does not contain any [IonSchemaModel.Constraint.TypeConstraint] constraints.
 */
internal fun IonSchemaModel.ConstraintList.getTypeConstraint(): IonSchemaModel.Constraint.TypeConstraint =
    this.getConstraint(IonSchemaModel.Constraint.TypeConstraint::class.java) as IonSchemaModel.Constraint.TypeConstraint

/**
 * Returns the first [IonSchemaModel.Constraint.AnyOf] in [this] constraint list. Throws an
 * [IllegalStateException] if [this] does not contain any [IonSchemaModel.Constraint.AnyOf] constraints.
 */
internal fun IonSchemaModel.ConstraintList.getAnyOfConstraint(): IonSchemaModel.Constraint.AnyOf =
    this.getConstraint(IonSchemaModel.Constraint.AnyOf::class.java) as IonSchemaModel.Constraint.AnyOf

/**
 * Returns the first [IonSchemaModel.Constraint.Element] in [this] constraint list. Throws an
 * [IllegalStateException] if [this] does not contain any [IonSchemaModel.Constraint.Element] constraints.
 */
internal fun IonSchemaModel.ConstraintList.getElementConstraint(): IonSchemaModel.Constraint.Element =
    this.getConstraint(IonSchemaModel.Constraint.Element::class.java) as IonSchemaModel.Constraint.Element

/**
 * Returns the first [IonSchemaModel.Constraint.Fields] in [this] constraint list. Throws an
 * [IllegalStateException] if [this] does not contain any [IonSchemaModel.Constraint.Fields] constraints.
 */
internal fun IonSchemaModel.ConstraintList.getFieldsConstraint(): IonSchemaModel.Constraint.Fields =
    this.getConstraint(IonSchemaModel.Constraint.Fields::class.java) as IonSchemaModel.Constraint.Fields

/**
 * Returns the first [IonSchemaModel.Constraint.ValidValues] in [this] constraint list. Throws an
 * [IllegalStateException] if [this] does not contain any [IonSchemaModel.Constraint.ValidValues] constraints.
 */
internal fun IonSchemaModel.ConstraintList.getValidValuesConstraint(): IonSchemaModel.Constraint.ValidValues =
    this.getConstraint(IonSchemaModel.Constraint.ValidValues::class.java) as IonSchemaModel.Constraint.ValidValues

/**
 * Returns the first [IonSchemaModel.Constraint.Scale] in [this] constraint list. Throws an
 * [IllegalStateException] if [this] does not contain any [IonSchemaModel.Constraint.Scale] constraints.
 */
internal fun IonSchemaModel.ConstraintList.getScaleConstraint(): IonSchemaModel.Constraint.Scale =
    this.getConstraint(IonSchemaModel.Constraint.Scale::class.java) as IonSchemaModel.Constraint.Scale

/**
 * Returns the first [IonSchemaModel.Constraint.Precision] in [this] constraint list. Throws an
 * [IllegalStateException] if [this] does not contain any [IonSchemaModel.Constraint.Precision] constraints.
 */
internal fun IonSchemaModel.ConstraintList.getPrecisionConstraint(): IonSchemaModel.Constraint.Precision =
    this.getConstraint(IonSchemaModel.Constraint.Precision::class.java) as IonSchemaModel.Constraint.Precision

/**
 * Returns the first [IonSchemaModel.Constraint.CodepointLength] in [this] constraint list. Throws an
 * [IllegalStateException] if [this] does not contain any [IonSchemaModel.Constraint.CodepointLength] constraints.
 */
internal fun IonSchemaModel.ConstraintList.getCodepointLengthConstraint(): IonSchemaModel.Constraint.CodepointLength =
    this.getConstraint(IonSchemaModel.Constraint.CodepointLength::class.java) as IonSchemaModel.Constraint.CodepointLength

/** Returns true if and only if [this] constraint list contains a constraint of type [constraintType] */
internal fun IonSchemaModel.ConstraintList.containsConstraint(constraintType: Class<out IonSchemaModel.Constraint>): Boolean =
    this.items.any { it.javaClass == constraintType }

/**
 * Returns true if and only if [this] constraint list has no constraints.
 */
internal fun IonSchemaModel.ConstraintList.isAny(): Boolean = this.items.isEmpty()

/**
 * Returns the first typename of [this] [IonSchemaModel.TypeReference]. [this] must be either an
 * [IonSchemaModel.TypeReference.NamedType] or an [IonSchemaModel.TypeReference.InlineType].
 */
internal fun IonSchemaModel.TypeReference.getTypename(): String =
    when (this) {
        is IonSchemaModel.TypeReference.InlineType -> this.firstNamedType().name.text
        is IonSchemaModel.TypeReference.NamedType -> this.name.text
        else -> error("Only InlineType and NamedType are supported")
    }

/**
 * Returns the first named type in [this] [IonSchemaModel.TypeReference.InlineType]'s constraints.
 */
private fun IonSchemaModel.TypeReference.InlineType.firstNamedType(): IonSchemaModel.TypeReference.NamedType =
    this.type.constraints.getTypeConstraint().type as IonSchemaModel.TypeReference.NamedType

/**
 * Returns [this] [IonSchemaModel.TypeReference] as an [IonSchemaModel.ConstraintList].
 */
internal fun IonSchemaModel.TypeReference.toConstraintList(): IonSchemaModel.ConstraintList =
    when (this) {
        is IonSchemaModel.TypeReference.InlineType -> this.type.constraints
        is IonSchemaModel.TypeReference.NamedType -> IonSchemaModel.build { constraintList(typeConstraint(this@toConstraintList)) }
        else -> error("Only InlineType and NamedType are supported")
    }

/**
 * Returns [this] [IonSchemaModel.ConstraintList] as an [IonSchemaModel.TypeReference].
 */
internal fun IonSchemaModel.ConstraintList.toTypeReference(): IonSchemaModel.TypeReference {
    val thisTypeConstraint = this.getTypeConstraint()
    return when (this.items.size) {
        1 -> thisTypeConstraint.type
        else -> {
            IonSchemaModel.build {
                inlineType(typeDefinition(constraints = this@toTypeReference), ionBool(false))
            }
        }
    }
}

/**
 * Returns true if and only if `this` struct is empty (i.e. has no [IonSchemaModel.Constraint.Fields] constraint).
 */
internal fun IonSchemaModel.ConstraintList.isEmptyStruct(): Boolean =
    !this.containsConstraint(IonSchemaModel.Constraint.Fields::class.java)

/**
 * Returns true if and only if [constraintList] contains the [IonSchemaModel.Constraint.AnyOf] constraint.
 */
internal fun hasUnion(constraintList: IonSchemaModel.ConstraintList): Boolean =
    constraintList.containsConstraint(IonSchemaModel.Constraint.AnyOf::class.java)

/**
 * Returns the first [IonSchemaModel.SchemaStatement.TypeStatement] from [this] [IonSchemaModel]'s statements. Throws
 * an [IllegalStateException] if [this] has no [IonSchemaModel.SchemaStatement.TypeStatement].
 */
internal fun IonSchemaModel.Schema.getFirstTypeStatement(): IonSchemaModel.SchemaStatement.TypeStatement {
    val statements = this.statements
    val typeStatement = statements.find { it is IonSchemaModel.SchemaStatement.TypeStatement }
        ?: throw IllegalStateException("Given schema $this has no TypeStatement")
    return typeStatement as IonSchemaModel.SchemaStatement.TypeStatement
}

/**
 * Unifies [this] sequence of [IonSchemaModel.ConstraintList]s to a unified [IonSchemaModel.ConstraintList] using
 * [unifier].
 */
internal fun Sequence<IonSchemaModel.ConstraintList>.unifiedConstraintList(unifier: ConstraintUnifier): IonSchemaModel.ConstraintList {
    return this.reduce { acc, typeConstraint ->
        unifier.unify(acc, typeConstraint)
    }
}

/**
 * Unifies the two [IonSchemaModel.NumberRule]s. If both number rules are equivalent and are
 * [IonSchemaModel.NumberRule.EqualsNumber], returns [numberRuleA] as an [IonSchemaModel.NumberRule.EqualsNumber].
 * Otherwise, returns an [IonSchemaModel.NumberRule.EqualsRange] of the combined number rules.
 */
internal fun unifyNumberRuleConstraints(numberRuleA: IonSchemaModel.NumberRule, numberRuleB: IonSchemaModel.NumberRule): IonSchemaModel.NumberRule {
    val aMin: Long
    val aMax: Long
    val bMin: Long
    val bMax: Long

    when (numberRuleA) {
        is IonSchemaModel.NumberRule.EqualsNumber -> {
            aMin = numberRuleA.value.longValue
            aMax = numberRuleA.value.longValue
        }
        is IonSchemaModel.NumberRule.EqualsRange -> {
            aMin = (numberRuleA.range.min as IonSchemaModel.NumberExtent.Inclusive).value.longValue
            aMax = (numberRuleA.range.max as IonSchemaModel.NumberExtent.Inclusive).value.longValue
        }
    }
    when (numberRuleB) {
        is IonSchemaModel.NumberRule.EqualsNumber -> {
            bMin = numberRuleB.value.longValue
            bMax = numberRuleB.value.longValue
        }
        is IonSchemaModel.NumberRule.EqualsRange -> {
            bMin = (numberRuleB.range.min as IonSchemaModel.NumberExtent.Inclusive).value.longValue
            bMax = (numberRuleB.range.max as IonSchemaModel.NumberExtent.Inclusive).value.longValue
        }
    }

    val newMin = min(aMin, bMin)
    val newMax = max(aMax, bMax)

    return if (newMin == newMax) {
        numberRuleA
    } else {
        IonSchemaModel.build { equalsRange(numberRange(inclusive(ionInt(newMin)), inclusive(ionInt(newMax)))) }
    }
}

/**
 * Returns [this] [IonSchemaModel.ConstraintList] with the [IonSchemaModel.Constraint.ClosedContent] constraint added.
 */
internal fun IonSchemaModel.ConstraintList.addClosedContentConstraint(): IonSchemaModel.ConstraintList =
    when (this.containsConstraint(IonSchemaModel.Constraint.ClosedContent::class.java)) {
        true -> this
        else -> {
            val constraints = this.items.toMutableList()
            constraints.add(IonSchemaModel.build { closedContent() })
            IonSchemaModel.build {
                constraintList(constraints)
            }
        }
    }

/**
 * Returns a [IonSchemaModel.Constraint.TypeConstraint] with [typeName] as a non-null named type.
 */
internal fun typeConstraintOf(typeName: String): IonSchemaModel.Constraint.TypeConstraint =
    IonSchemaModel.build { typeConstraint(namedType(name = typeName, nullable = ionBool(false))) }
