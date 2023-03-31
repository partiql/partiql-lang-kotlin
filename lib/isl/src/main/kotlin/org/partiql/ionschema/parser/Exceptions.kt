package org.partiql.ionschema.parser

import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.IonLocation
import com.amazon.ionelement.api.location
import com.amazon.ionschema.IonSchemaException

class IonSchemaParseException(val location: IonLocation?, val error: Error) :
    IonSchemaException(getMessage(location, error.message))

private fun getMessage(blame: IonLocation?, message: String): String {
    val location = blame ?: "<unknown location>"
    return "$location: $message"
}

internal fun parseError(blame: IonElement, error: Error): Nothing =
    throw IonSchemaParseException(blame.metas.location, error)

internal fun parseError(blame: IonLocation?, error: Error): Nothing =
    throw IonSchemaParseException(blame, error)

data class ModelValidationError(
    val component: String,
    val actualType: ElementType,
    val expectedTypes: List<ElementType>
) {
    internal fun makeMessage(): String {
        val typesString = expectedTypes.joinToString { it.toString() }
        return "Expected $component to be (one of) $typesString instead of $actualType"
    }
}

// model validation errors generally do not have source locations.
internal fun modelValidationError(
    component: String,
    actualType: ElementType,
    expectedTypes: List<ElementType>
): Nothing {
    throw IonSchemaModelValidationError(ModelValidationError(component, actualType, expectedTypes))
}

class IonSchemaModelValidationError(val error: ModelValidationError) :
    IonSchemaException(error.makeMessage())
