package org.partiql.coverage.api.impl

import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.TestTemplateInvocationContext

/**
 * @since 5.0
 */
internal class PartiQLTestInvocationContext(
    private val formatter: PartiQLTestNameFormatter,
    private val methodContext: PartiQLTestMethodContext,
    private val arguments: Array<Any>,
    private val invocationIndex: Int
) : TestTemplateInvocationContext {
    override fun getDisplayName(invocationIndex: Int): String {
        return formatter.format(invocationIndex, *arguments)
    }

    override fun getAdditionalExtensions(): List<Extension> {
        return listOf(
            PartiQLTestParameterResolver(methodContext, arguments, invocationIndex)
        )
    }
}