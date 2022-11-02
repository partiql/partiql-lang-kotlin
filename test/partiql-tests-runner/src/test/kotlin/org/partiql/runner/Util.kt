package org.partiql.runner

import com.amazon.ion.IonContainer
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.namedValue
import org.partiql.lang.eval.stringExprValue
import org.partiql.lang.eval.structExprValue
import org.partiql.lang.eval.toExprValue
import org.partiql.lang.eval.toIonValue

// TODO: remove this file once we remove the prefix 'paritql_' as annotation of Ion values in core package

const val BAG_ANNOTATION = "\$bag"
const val MISSING_ANNOTATION = "\$missing"

/**
 * Converts the conformance test's encoding of PartiQL values in Ion to an [ExprValue]. The conformance tests have a
 * slightly different encoding than the default conversion function provided by [ExprValueFactory]. E.g. Ion value
 * annotation for bag.
 */
internal fun IonValue.toExprValueChangingAnnotation(): ExprValue {
    // Need to create a different IonValue to ExprValue conversion function because the default provided by
    // `ExprValueFactory`'s [newFromIonValue] relies on a different encoding of PartiQL-specific types than the
    // conformance tests (e.g. `ExprValueFactory` uses $partiql_bag rather than $bag)
    changeAnnotation()
    return when (this) {
        is IonStruct -> {
            structExprValue(map { it.toExprValueChangingAnnotation().namedValue(stringExprValue(it.fieldName)) }, StructOrdering.UNORDERED)
        }
        else -> toExprValue()
    }
}

private fun IonValue.changeAnnotation() {
    when {
        hasTypeAnnotation(BAG_ANNOTATION) -> {
            removeTypeAnnotation(BAG_ANNOTATION)
            addTypeAnnotation("\$partiql_bag")
        }
        hasTypeAnnotation(MISSING_ANNOTATION) -> {
            removeTypeAnnotation(MISSING_ANNOTATION)
            addTypeAnnotation("\$partiql_missing")
        }
    }

    if (this is IonContainer) {
        forEach { it.changeAnnotationToPartiql() }
    }
}

/**
 * Converts an [ExprValue] to the conformance test suite's modeling of PartiQL values in Ion.
 */
internal fun ExprValue.toIonValueChangingAnnotation(ion: IonSystem): IonValue =
    toIonValue(ion).apply { changeAnnotationToPartiql() }

private fun IonValue.changeAnnotationToPartiql() {
    when {
        hasTypeAnnotation("\$partiql_bag") -> {
            removeTypeAnnotation("\$partiql_bag")
            addTypeAnnotation(BAG_ANNOTATION)
        }
        hasTypeAnnotation("\$partiql_missing") -> {
            removeTypeAnnotation("\$partiql_missing")
            addTypeAnnotation(MISSING_ANNOTATION)
        }
    }

    if (this is IonContainer) {
        forEach { it.changeAnnotationToPartiql() }
    }
}
