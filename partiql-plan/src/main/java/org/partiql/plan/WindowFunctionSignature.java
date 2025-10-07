package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * Represents the signature of a window function.
 * @see WindowFunctionNode
 */
public final class WindowFunctionSignature {

    private final String name;
    private final List<PType> parameterTypes;
    private final PType returnType;
    private final boolean ignoreNulls;

    /**
     * Constructs a new {@link WindowFunctionSignature}.
     * @param name the name of the function
     * @param parameterTypes the types of the parameters
     * @param returnType the type of the return value
     * @param ignoreNulls whether the function should ignore nulls
     */
    public WindowFunctionSignature(
            @NotNull String name,
            @NotNull List<PType> parameterTypes,
            @NotNull PType returnType,
            boolean ignoreNulls
    ) {
        this.name = name;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.ignoreNulls = ignoreNulls;
    }

    /**
     * Returns the name of the function.
     * @return the name of the function
     */
    @NotNull
    public String getName() {
        return this.name;
    }

    /**
     * Returns the types of the parameters.
     * @return the types of the parameters
     */
    @NotNull
    public List<PType> getParameterTypes() {
        return this.parameterTypes;
    }

    /**
     * Returns the type of the return value.
     * @return the type of the return value
     */
    @NotNull
    public PType getReturnType() {
        return this.returnType;
    }

    /**
     * Returns whether the function should ignore nulls.
     * @return whether the function should ignore nulls
     */
    public boolean isIgnoreNulls() {
        return this.ignoreNulls;
    }
}
