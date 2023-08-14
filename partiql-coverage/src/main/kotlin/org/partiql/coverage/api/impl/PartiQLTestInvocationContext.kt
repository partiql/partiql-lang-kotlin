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

package org.partiql.coverage.api.impl

import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.TestTemplateInvocationContext

/**
 * Represents the context of a single invocation of a PartiQL Test.
 */
internal class PartiQLTestInvocationContext(
    private val methodContext: PartiQLTestMethodContext,
    private val arguments: Array<Any>,
    private val invocationIndex: Int
) : TestTemplateInvocationContext {

    override fun getDisplayName(invocationIndex: Int): String {
        return invocationIndex.toString()
    }

    override fun getAdditionalExtensions(): List<Extension> = listOf(
        PartiQLTestParameterResolver(methodContext, arguments, invocationIndex)
    )
}
