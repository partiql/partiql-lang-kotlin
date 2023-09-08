/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.binding

import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.Ident
import org.partiql.lang.eval.err
import org.partiql.lang.util.propertyValueMapOf

// SQL-ids-TODO Make this print something useful when called. (Refer to the call site.)
// Now that "case-sensitive lookup" is gone, the only situation when this is called
// is for an Ion record where the same field name is used in multiple fields,
// but the error created by this function was designed to highlight different names.
// (That is, it was not useful in this case prior to SQL-ids transition either.)
internal fun errAmbiguousBinding(bindingName: Ident, matchingNames: List<String>): Nothing {
    err(
        "Multiple matches were found for the specified identifier",
        ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
        propertyValueMapOf(
            Property.BINDING_NAME to bindingName,
            Property.BINDING_NAME_MATCHES to matchingNames.joinToString(", ")
        ),
        internal = false
    )
}
