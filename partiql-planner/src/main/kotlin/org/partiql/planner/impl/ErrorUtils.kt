/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.planner.impl

import com.amazon.ion.IonValue
import com.amazon.ionelement.api.MetaContainer
import org.partiql.errors.ErrorCode
import org.partiql.errors.Problem
import org.partiql.errors.ProblemHandler
import org.partiql.errors.Property
import org.partiql.errors.PropertyValueMap
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.pig.runtime.DomainNode
import org.partiql.planner.PlannerException
import org.partiql.planner.PlanningProblemDetails

/**
 * Shorthand for throwing function evaluation. Separated from [err] to avoid loosing the context unintentionally
 */
internal fun errNoContext(message: String, errorCode: ErrorCode, internal: Boolean): Nothing =
    err(message, errorCode, PropertyValueMap(), internal)

/** Shorthand for throwing evaluation with context with an error code.. */
internal fun err(message: String, errorCode: ErrorCode, errorContext: PropertyValueMap, internal: Boolean): Nothing =
    throw PlannerException(message, errorCode, errorContext, internal = internal)

internal fun errorContextFrom(location: SourceLocationMeta?): PropertyValueMap {
    val errorContext = PropertyValueMap()
    if (location != null) {
        fillErrorContext(errorContext, location)
    }
    return errorContext
}

internal fun fillErrorContext(errorContext: PropertyValueMap, location: SourceLocationMeta?) {
    if (location != null) {
        errorContext[Property.LINE_NUMBER] = location.lineNum
        errorContext[Property.COLUMN_NUMBER] = location.charOffset
    }
}

/**
 * Returns the [SourceLocationMeta] as an error context if the [SourceLocationMeta.TAG] exists in the passed
 * [metaContainer]. Otherwise, returns an empty map.
 */
internal fun errorContextFrom(metaContainer: MetaContainer?): PropertyValueMap {
    if (metaContainer == null) {
        return PropertyValueMap()
    }
    val location = metaContainer[SourceLocationMeta.TAG] as? SourceLocationMeta
    return if (location != null) {
        errorContextFrom(location)
    } else {
        PropertyValueMap()
    }
}

/**
 * Convenience function that logs [PlanningProblemDetails.UnimplementedFeature] to the receiver [ProblemHandler]
 * handler, putting [blame] on the specified [DomainNode] (this is the superclass of all PIG-generated types).
 * "Blame" in this case, means that the line & column number in the metas of [blame] become the problem's.
 */
internal fun ProblemHandler.handleUnimplementedFeature(blame: DomainNode, featureName: String) =
    this.handleProblem(createUnimplementedFeatureProblem(blame, featureName))

private fun createUnimplementedFeatureProblem(blame: DomainNode, featureName: String) =
    Problem(
        (blame.metas.sourceLocationMeta?.toProblemLocation() ?: UNKNOWN_PROBLEM_LOCATION),
        PlanningProblemDetails.UnimplementedFeature(featureName)
    )

internal fun errAstNotNormalized(message: String): Nothing =
    error("$message - have the basic visitor transforms been executed first?")

/**
 * Helper function to reduce the syntactical overhead of creating a [PropertyValueMap].
 *
 * This overload accepts [line] and [column] arguments before other properties.
 */
internal fun propertyValueMapOf(
    line: Int,
    column: Int,
    vararg otherProperties: Pair<Property, Any>
): PropertyValueMap =
    propertyValueMapOf(
        *otherProperties,
        Property.LINE_NUMBER to line.toLong(),
        Property.COLUMN_NUMBER to column.toLong()
    )

/**
 * Helper function to reduce the syntactical overhead of creating a [PropertyValueMap].
 */
internal fun propertyValueMapOf(vararg properties: Pair<Property, Any>): PropertyValueMap {
    val pvm = PropertyValueMap()
    properties.forEach {
        if (pvm.hasProperty(it.first)) throw IllegalArgumentException("Duplicate property: ${it.first.propertyName}")
        when (it.second) {
            is Int -> pvm[it.first] = it.second as Int
            is Long -> pvm[it.first] = it.second as Long
            is String -> pvm[it.first] = it.second as String
            is IonValue -> pvm[it.first] = it.second as IonValue
            is Enum<*> -> pvm[it.first] = it.second.toString()
            else -> throw IllegalArgumentException("Cannot convert ${it.second.javaClass.name} to PropertyValue")
        }
    }

    return pvm
}

internal fun quotationHint(caseSensitive: Boolean) =
    if (caseSensitive) {
        // Individuals that are new to SQL often try to use double quotes for string literals.
        // Let's help them out a bit.
        " Hint: did you intend to use single-quotes (') here?  Remember that double-quotes (\") denote " +
            "quoted identifiers and single-quotes denote strings."
    } else {
        ""
    }

/**
 * Adds [Property.LINE_NUMBER] and [Property.COLUMN_NUMBER] to the [PropertyValueMap] if the [SourceLocationMeta.TAG]
 * is present in the passed [metas]. Otherwise, returns the unchanged [PropertyValueMap].
 */
internal fun PropertyValueMap.addSourceLocation(metas: MetaContainer): PropertyValueMap {
    (metas[SourceLocationMeta.TAG] as? SourceLocationMeta)?.let {
        this[Property.LINE_NUMBER] = it.lineNum
        this[Property.COLUMN_NUMBER] = it.charOffset
    }
    return this
}
