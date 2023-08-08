package org.partiql.coverage.api.impl

import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.TestTemplateInvocationContext

internal class PartiQLTestInvocationContext(
    private val methodContext: PartiQLTestMethodContext,
    private val arguments: Array<Any>,
    private val invocationIndex: Int
) : TestTemplateInvocationContext {

    private val formatter = PartiQLTestNameFormatter

    override fun getDisplayName(invocationIndex: Int): String {
        return formatter.format(invocationIndex, *arguments)
    }

    override fun getAdditionalExtensions(): List<Extension> = listOf(
        PartiQLTestParameterResolver(methodContext, arguments, invocationIndex)
    )
}