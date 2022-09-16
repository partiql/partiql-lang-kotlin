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

package org.partiql.lang.syntax

/** Simple source line/column value. */
@Deprecated(
    message = "This class is subject to removal.",
    level = DeprecationLevel.WARNING
) // To be removed before 1.0
data class SourcePosition(val line: Long, val column: Long) {
    override fun toString(): String = "line $line, column $column"
}
