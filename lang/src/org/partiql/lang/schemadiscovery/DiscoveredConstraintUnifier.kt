package org.partiql.lang.schemadiscovery

import org.partiql.ionschema.model.IonSchemaModel
import java.lang.IllegalArgumentException

/**
 * For two conflicting [IonSchemaModel.ConstraintList]s with the same type constraint, unifies the constraint
 * lists' additional discovered constraints (i.e. not one of:
 *  - [IonSchemaModel.Constraint.TypeConstraint]
 *  - [IonSchemaModel.Constraint.Fields] for structs
 *  - [IonSchemaModel.Constraint.ClosedContent] for structs
 *  - [IonSchemaModel.Constraint.Element] for sequences).
 *  
 * This is intended to be called by a [ConstraintUnifier] when unifying 
 *  - discovered constraints only ([MultipleTypedDCU])
 *  - discovered with definite constraints ([AppendAdditionalConstraints])
 */
internal fun interface DiscoveredConstraintUnifier {
    operator fun invoke(a: IonSchemaModel.ConstraintList, b: IonSchemaModel.ConstraintList): IonSchemaModel.ConstraintList
}

/**
 * Represents a [DiscoveredConstraintUnifier] where each [IonSchemaModel.ConstraintList] to unify has a
 * [IonSchemaModel.Constraint.TypeConstraint] with [typeName]. This is intended to be used when creating 
 * [MultipleTypedDCU].
 */
internal data class SingleTypedDCU(val typeName: String, val unifyFunc: DiscoveredConstraintUnifier)

/**
 * For two conflicting constraint lists, `a` and `b`, unifies discovered constraints based on [constraintUnifiers]. 
 * If `a`/`b`'s type name matches one of the [constraintUnifiers]' [SingleTypedDCU.typeName]s, then `a` and `b` are 
 * unified with that corresponding unifier. Otherwise, an empty constraint list is returned.
 *
 * @exception IllegalArgumentException if any of [constraintUnifiers] have the same
 * [SingleTypedDCU.typeName].
 */
internal class MultipleTypedDCU(
    private val constraintUnifiers: List<SingleTypedDCU> = standardTypedDiscoveredConstraintUnifiers
): DiscoveredConstraintUnifier {
    private val discoveredConstraintUnifierMapping = initializeMapping()

    private fun initializeMapping(): Map<String, DiscoveredConstraintUnifier> {
        val mapping = mutableMapOf<String, DiscoveredConstraintUnifier>()
        constraintUnifiers.forEach {
            if (mapping.containsKey(it.typeName)) {
                throw IllegalArgumentException("${it.typeName} is a repeated type name for MultipleTypedDCU")
            }
            mapping[it.typeName] = it.unifyFunc
        }
        return mapping
    }

    override fun invoke(a: IonSchemaModel.ConstraintList, b: IonSchemaModel.ConstraintList): IonSchemaModel.ConstraintList {
        val typeName = a.getTypeConstraint().type.getTypename()
        return when (val unifier = discoveredConstraintUnifierMapping[typeName]) {
            null -> emptyConstraintList
            else -> unifier(a, b)
        }
    }
}

/**
 * For two conflicting constraint lists, `a` and `b`, appends `b`'s constraints not found in `a`. Any constraints that
 * are found in `a` and `b` will return `a`'s constraint.
 */
internal class AppendAdditionalConstraints: DiscoveredConstraintUnifier {
    private fun IonSchemaModel.Constraint.isDiscoveredConstraint(): Boolean {
        return this !is IonSchemaModel.Constraint.TypeConstraint
            && this !is IonSchemaModel.Constraint.ClosedContent
            && this !is IonSchemaModel.Constraint.Fields
            && this !is IonSchemaModel.Constraint.Element
    }

    override fun invoke(a: IonSchemaModel.ConstraintList, b: IonSchemaModel.ConstraintList): IonSchemaModel.ConstraintList {
        val constraints = mutableListOf<IonSchemaModel.Constraint>()

        val aConstraints = a.items.filter { it.isDiscoveredConstraint() }
        val bConstraints = b.items.filter { it.isDiscoveredConstraint() }

        // keep all of `a`'s discovered constraints
        constraints.addAll(aConstraints)
        // add `b`'s constraints that are not in `a`
        bConstraints.forEach { bConstraint ->
            if (constraints.all { it.javaClass != bConstraint.javaClass }) {
                constraints.add(bConstraint)
            }
        }
        return IonSchemaModel.build { constraintList(constraints) }
    }
}

/**
 * For [TypeConstraint.INT], merges the [IonSchemaModel.Constraint.ValidValues] ranges. If either do not have the
 * valid_values constraint, an empty constraint list is returned.
 */
internal val INT_VALID_VALUES_UNIFIER = SingleTypedDCU(TypeConstraint.INT.typeName) { a, b ->
    val constraintList = mutableListOf<IonSchemaModel.Constraint>()

    val aHasValidValuesConstraint = a.containsConstraint(IonSchemaModel.Constraint.ValidValues::class.java)
    val bHasValidValuesConstraint = b.containsConstraint(IonSchemaModel.Constraint.ValidValues::class.java)

    // constraints are both not unconstrained
    if (aHasValidValuesConstraint && bHasValidValuesConstraint) {
        val aValidValuesConstraint = a.getValidValuesConstraint()
        val bValidValuesConstraint = b.getValidValuesConstraint()

        if (aValidValuesConstraint == INT8_RANGE_CONSTRAINT || bValidValuesConstraint == INT8_RANGE_CONSTRAINT) {
            constraintList.add(INT8_RANGE_CONSTRAINT)
        }
        // else, constraints differ, so one must be int2 and the other int4. Thus, int4 is returned
        else {
            constraintList.add(INT4_RANGE_CONSTRAINT)
        }
    }
    IonSchemaModel.build { constraintList(constraintList) }
}

/**
 * For [TypeConstraint.DECIMAL], merges the [IonSchemaModel.Constraint.Scale] and [IonSchemaModel.Constraint.Precision]
 * ranges.
 *
 * @exception IllegalStateException if either of the constraint lists do not have scale or precision constraints.
 */
internal val DECIMAL_SCALE_AND_PRECISION_UNIFIER = SingleTypedDCU(TypeConstraint.DECIMAL.typeName) { a, b ->
    val constraintList = mutableListOf<IonSchemaModel.Constraint>()

    val aScale = a.getScaleConstraint().rule
    val bScale = b.getScaleConstraint().rule
    constraintList.add(IonSchemaModel.build { scale(unifyNumberRuleConstraints(aScale, bScale)) })

    val aPrecision = a.getPrecisionConstraint().rule
    val bPrecision = b.getPrecisionConstraint().rule
    constraintList.add(IonSchemaModel.build { precision(unifyNumberRuleConstraints(aPrecision, bPrecision)) })
    IonSchemaModel.build { constraintList(constraintList) }
}

/**
 * For [TypeConstraint.STRING], merges the [IonSchemaModel.Constraint.CodepointLength] ranges.
 *
 * @exception IllegalStateException if either of the constraint lists do not have the codepoint_length constraint.
 */
internal val STRING_CODEPOINT_LENGTH_UNIFIER = SingleTypedDCU(TypeConstraint.STRING.typeName) { a, b ->
    val aLength = a.getCodepointLengthConstraint().rule
    val bLength = b.getCodepointLengthConstraint().rule
    IonSchemaModel.build { constraintList(codepointLength(unifyNumberRuleConstraints(aLength, bLength))) }
}

/**
 * List of [SingleTypedDCU]s, composed of:
 *  [INT_VALID_VALUES_UNIFIER]- unifies INT's valid_values constraint,
 *  [DECIMAL_SCALE_AND_PRECISION_UNIFIER]- unifies DECIMAL's scale and precision constraints,
 *  [STRING_CODEPOINT_LENGTH_UNIFIER]- unifies STRING's codepoint_length constraint
 */
internal val standardTypedDiscoveredConstraintUnifiers =
    listOf(
        INT_VALID_VALUES_UNIFIER,
        DECIMAL_SCALE_AND_PRECISION_UNIFIER,
        STRING_CODEPOINT_LENGTH_UNIFIER)
