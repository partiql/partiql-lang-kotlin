
package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.ElementType
import org.partiql.lang.domains.PartiqlPhysical

/**
 * This is the semantic version number of the logical and physical plans supported by this version of PartiQL.  This
 * deals only with compatibility of trees that have been persisted as s-expressions with their PIG-generated
 * classes. The format is: `<major>.<minor>`.  One or both of these will need to be changed when the following
 * events happen:
 *
 * - Increment `<major>` and set `<minor>` to `0` when a change to `partiql.ion` is introduced that will cause the
 * persisted s-expressions to fail to load under the new version.  Examples include:
 *     - Making an element non-nullable that was previously nullable.
 *     - Renaming any type or sum variant.
 *     - Removing a sum variant.
 *     - Adding or removing any element of any product type.
 *     - Changing the data type of any element.
 *     - Adding a required field to a record type.
 * - Increment `<minor>` when a change to `partiql.ion` is introduced that will *not* cause the persisted s-expressions
 * to fail to load into the PIG-generated classes.  Examples include:
 *     - Adding a new, optional (nullable) field to a record type.
 *     - Adding a new sum variant.
 *     - Changing an element that was previously non-nullable nullable.
 *
 * It would be nice to embed semantic version in the PIG type universe somehow, but this isn't yet implemented, so we
 * have to include it here for now.  See: https://github.com/partiql/partiql-ir-generator/issues/121
 */
const val PLAN_VERSION_NUMBER = "0.0"

internal fun errAstNotNormalized(message: String): Nothing =
    error("$message - have the basic visitor transforms been executed first?")

/** Returns true if the given expression is `(lit true)`. */
fun PartiqlPhysical.Expr.isLitTrue() =
    when (this) {
        is PartiqlPhysical.Expr.Lit -> this.value.type == ElementType.BOOL && this.value.booleanValue
        else -> false
    }
