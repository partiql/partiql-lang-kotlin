package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * <p>
 * Represents a parameter for a routine provider.
 * </p>
 * <p>
 * This is different from {@link Parameter} as it does not represent the same thing. It is specifically used for
 * routine providers, not routines. This allows  for the sorting of routine providers based on their preferred types.
 * This <i>may</i>, in the future, also add additional methods that aid in debugging/error messaging.
 * </p>
 * @see RoutineProviderSignature
 * @see AggProvider
 * @see FnProvider
 */
public final class RoutineProviderParameter {
    // DEVELOPER NOTE:
    // This is different from Parameter, as it does not represent the same thing. It is specifically used for
    // RoutineProviders, not Routines.

    private final String name;
    private final PType type;

    /**
     * Creates a new {@link RoutineProviderParameter} with the given name and type.
     * @param name the name of the parameter
     * @param type the type of the parameter
     */
    public RoutineProviderParameter(@NotNull String name, @NotNull PType type) {
        this.name = name;
        this.type = type;
    }

    /**
     * The name of the parameter.
     * @return the name of the parameter.
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the preferred type of the parameter. This is used solely for the sorting of {@link FnProvider} and
     * {@link AggProvider}.
     * @return the preferred type of the parameter. This is used solely for the sorting of {@link FnProvider}
     * and {@link AggProvider}.
     */
    @NotNull
    public PType getType() {
        return type;
    }
}
