package org.partiql.runner

import com.amazon.ion.IonList
import com.amazon.ion.IonNull
import com.amazon.ion.IonSexp
import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.namedValue

const val BAG_ANNOTATION = "\$bag"
const val MISSING_ANNOTATION = "\$missing"

/**
 * Converts the conformance test's encoding of PartiQL values in Ion to an [ExprValue]. The conformance tests have a
 * slightly different encoding than the default conversion function provided by [ExprValueFactory]. E.g. Ion value
 * annotation for bag.
 */
internal fun IonValue.toExprValue(exprValueFactory: ExprValueFactory): ExprValue {
    // Need to create a different IonValue to ExprValue conversion function because the default provided by
    // `ExprValueFactory`'s [newFromIonValue] relies on a different encoding of PartiQL-specific types than the
    // conformance tests (e.g. `ExprValueFactory` uses $partiql_bag rather than $bag)
    val elem = this
    val annotations = elem.typeAnnotations
    return when {
        (elem is IonList) && annotations.contains(BAG_ANNOTATION) -> {
            val elemsAsExprValues = elem.map {
                it.toExprValue(exprValueFactory)
            }
            exprValueFactory.newBag(elemsAsExprValues)
        }
        elem is IonNull && elem.hasTypeAnnotation(MISSING_ANNOTATION) -> exprValueFactory.missingValue
        // TODO: other PartiQL types not in Ion
        elem is IonList -> exprValueFactory.newList(elem.map { it.toExprValue(exprValueFactory) })
        elem is IonSexp -> exprValueFactory.newSexp(elem.map { it.toExprValue(exprValueFactory) })
        elem is IonStruct -> {
            exprValueFactory.newStruct(elem.map { it.toExprValue(exprValueFactory).namedValue(exprValueFactory.newString(it.fieldName)) }, StructOrdering.UNORDERED)
        }
        else -> exprValueFactory.newFromIonValue(elem)
    }
}
