package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Represents a window function node.
 * @see WindowFunctionSignature
 */
public final class WindowFunctionNode {

    private final WindowFunctionSignature signature;
    private final List<Rex> arguments;

    /**
     * Constructs a new {@link WindowFunctionNode}.
     * @param signature the signature of the window function
     * @param arguments the arguments of the window function
     */
    public WindowFunctionNode(@NotNull WindowFunctionSignature signature, @NotNull List<Rex> arguments) {
        this.signature = signature;
        this.arguments = arguments;
    }

    /**
     * Returns the signature of the window function.
     * @return the signature of the window function
     */
    @NotNull
    public WindowFunctionSignature getSignature() {
        return signature;
    }

    /**
     * Returns the arguments of the window function.
     * @return the arguments of the window function
     */
    @NotNull
    public List<Rex> getArguments() {
        return arguments;
    }
}
