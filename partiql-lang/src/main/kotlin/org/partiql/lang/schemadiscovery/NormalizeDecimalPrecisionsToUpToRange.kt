package org.partiql.lang.schemadiscovery

import com.amazon.ionelement.api.ionInt
import org.partiql.ionschema.model.IonSchemaModel

/**
 * Normalizes decimal precisions ([IonSchemaModel.Constraint.Precision]) to an "upto" range. For exact precision p,
 * returns an inclusive range from 1 to p. For a ranged precision with an inclusive max, returns an inclusive range
 * from 1 to max.
 *
 * Because the dataguide's default [ConstraintDiscoverer] for decimals infers exact precisions and ranges, some use
 * cases would just like to infer a range from 1 (inclusive) to the max, inclusive precision (i.e. "upto" inclusive
 * range). This [IonSchemaModel.VisitorTransform] normalizes such decimal precisions to an inclusive "upto" range.
 */
class NormalizeDecimalPrecisionsToUpToRange : IonSchemaModel.VisitorTransform() {
    override fun transformConstraintPrecision(node: IonSchemaModel.Constraint.Precision): IonSchemaModel.Constraint {
        val transformedPrecisionRule = when (val nodeNumberRule = node.rule) {
            is IonSchemaModel.NumberRule.EqualsNumber -> IonSchemaModel.build {
                equalsRange(numberRange(inclusive(ionInt(1)), inclusive(nodeNumberRule.value)))
            }
            is IonSchemaModel.NumberRule.EqualsRange -> IonSchemaModel.build {
                val maxValue = when (val nodeNumberMax = nodeNumberRule.range.max) {
                    is IonSchemaModel.NumberExtent.Inclusive -> nodeNumberMax.value
                    else -> error("Unsupported number range for normalization")
                }
                equalsRange(numberRange(inclusive(ionInt(1)), inclusive(maxValue)))
            }
        }
        return IonSchemaModel.build { precision(transformedPrecisionRule) }
    }
}
