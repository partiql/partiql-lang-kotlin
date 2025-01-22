package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * <p>
 * This represents the signature of a routine provider. This is distinct from {@link RoutineSignature}, as it is specific
 * to the provider, and not the instance. The provider signature is used to determine if a routine provider is applicable
 * to a given call site, and if so, which routine provider to use.
 * </p>
 * <p>
 * While this is currently the same as a {@link RoutineSignature}, additional methods <i>may</i> be added to this class
 * for future use, such as for retrieving all representative signatures of an implementation (for debugging purposes
 * or for error-messaging).
 * </p>
 */
public final class RoutineProviderSignature {
    @NotNull
    private final String name;
    @NotNull
    private final List<RoutineProviderParameter> params;

    /**
     * Creates a new {@link RoutineProviderSignature} with the given name and parameters.
     * @param name the name of the function
     * @param params the parameters of the function
     */
    public RoutineProviderSignature(@NotNull String name, @NotNull List<RoutineProviderParameter> params) {
        this.name = name;
        this.params = params;
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
        return params.size();
    }

    /**
     * Returns the parameters of the function.
     * @return the parameters of the function
     */
    public List<RoutineProviderParameter> getParameters() {
        return params;
    }
}
