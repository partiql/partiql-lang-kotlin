package org.partiql.lang.eval.physical

import com.amazon.ionelement.api.MetaContainer
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.util.propertyValueMapOf

private const val UNBOUND_QUOTED_IDENTIFIER_HINT: String =
    "Hint: did you intend to use single quotes (') here instead of double quotes (\")? " +
        "Use single quotes (') for string literals and double quotes (\") for quoted identifiers."

internal fun throwUndefinedVariableException(
    bindingName: BindingName,
    metas: MetaContainer?
): Nothing {
    val (errorCode, hint) = when (bindingName.bindingCase) {
        BindingCase.SENSITIVE ->
            ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST to " $UNBOUND_QUOTED_IDENTIFIER_HINT"
        BindingCase.INSENSITIVE ->
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST to ""
    }
    throw EvaluationException(
        message = "No such binding: ${bindingName.name}.$hint",
        errorCode = errorCode,
        errorContext = (metas?.let { errorContextFrom(metas) } ?: propertyValueMapOf()).also {
            it[Property.BINDING_NAME] = bindingName.name
        },
        internal = false
    )
}
