package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * <p>
 * This represents the signature of a routine provider. This is distinct from {@link RoutineSignature}, as it is specific
 * to the provider, and not the instance. The provider signature is used to determine if a routine provider is applicable
 * to a given call site, and if so, which routine provider to use.
 * </p>
 * <p>
 * This differs from {@link RoutineSignature}, as it does not have {@link RoutineSignature#isNullCall()} and
 * {@link RoutineSignature#isMissingCall()}, among others.
 * </p>
 */
public final class RoutineProviderSignature {
    @NotNull
    private final String name;
    @NotNull
    private final List<PType> paramTypes;

    /**
     * Creates a new {@link RoutineProviderSignature} with the given name and parameters.
     * @param name the name of the function
     * @param parameterTypes the types of the parameters of the function
     */
    public RoutineProviderSignature(@NotNull String name, @NotNull List<PType> parameterTypes) {
        this.name = name;
        this.paramTypes = parameterTypes;
    }

    /**
     * Returns the name of the function.
     * @return the name of the function
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the number of parameters that the function takes.
     * @return the number of parameters that the function takes
     */
    public int getArity() {
        return paramTypes.size();
    }

    /**
     * Returns the preferred types of the parameters of the function. This is used for the sorting of {@link FnProvider}
     * and {@link AggProvider}.
     * @return the preferred types of the parameters of the function
     */
    public List<PType> getParameterTypes() {
        return paramTypes;
    }
}
