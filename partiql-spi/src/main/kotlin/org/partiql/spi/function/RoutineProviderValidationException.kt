/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.spi.function

import java.util.Collections

/**
 * Reports one or more invalid routine-provider definitions.
 */
public class RoutineProviderValidationException private constructor(
    issues: Collection<RoutineProviderValidationIssue>,
    cause: Throwable? = null,
) : IllegalArgumentException(
    "Routine provider validation failed with ${issues.size} issue(s).",
    cause,
) {
    public val issues: List<RoutineProviderValidationIssue> =
        Collections.unmodifiableList(ArrayList(issues))

    internal companion object {

        @JvmSynthetic
        internal fun create(
            issues: Collection<RoutineProviderValidationIssue>,
            cause: Throwable? = null,
        ): RoutineProviderValidationException =
            RoutineProviderValidationException(issues, cause)
    }
}
