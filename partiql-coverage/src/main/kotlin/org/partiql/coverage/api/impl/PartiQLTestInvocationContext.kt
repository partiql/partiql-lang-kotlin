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
