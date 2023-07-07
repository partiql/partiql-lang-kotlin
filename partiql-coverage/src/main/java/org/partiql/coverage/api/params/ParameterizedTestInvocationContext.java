package org.partiql.coverage.api.params;

import static java.util.Collections.singletonList;

import java.util.List;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

/**
 * @since 5.0
 */
class ParameterizedTestInvocationContext implements TestTemplateInvocationContext {

    private final ParameterizedTestNameFormatter formatter;
    private final ParameterizedTestMethodContext methodContext;
    private final Object[] arguments;
    private final int invocationIndex;

    ParameterizedTestInvocationContext(ParameterizedTestNameFormatter formatter,
                                       ParameterizedTestMethodContext methodContext, Object[] arguments, int invocationIndex) {
        this.formatter = formatter;
        this.methodContext = methodContext;
        this.arguments = arguments;
        this.invocationIndex = invocationIndex;
    }

    @Override
    public String getDisplayName(int invocationIndex) {
        return this.formatter.format(invocationIndex, this.arguments);
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
        return singletonList(
                new ParameterizedTestParameterResolver(this.methodContext, this.arguments, this.invocationIndex));
    }

}
