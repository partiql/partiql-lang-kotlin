package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * Represents a function parameter for a routine.
 * @see RoutineSignature
 * @see Fn
 * @see Agg
 */
public final class Parameter {

    private final String name;
    private final PType type;

    /**
     * Creates a new {@link Parameter} with the given name and type.
     * @param name the name of the parameter
     * @param type the type of the parameter
     */
    public Parameter(@NotNull String name, @NotNull PType type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Returns the name of the parameter.
     * @return the name of the parameter.
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the type of the parameter.
     * @return the type of the parameter.
     */
    @NotNull
    public PType getType() {
        return type;
    }
}
