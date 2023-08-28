package org.partiql.spi.function

import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Represents a function interface that can be overridden by external teams.
 *
 * An implementation of this interface defines the behavior of the function
 * and its signature, which includes the function's names, return type, parameters,
 * determinism, and an optional description.
 */
@PartiQLFunctionExperimental
public interface PartiQLFunction {
    public val signature: FunctionSignature
    @OptIn(PartiQLValueExperimental::class)
    public fun invoke(session: ConnectorSession, arguments: List<PartiQLValue>): PartiQLValue
}
