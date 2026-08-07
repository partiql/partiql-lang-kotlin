package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * This represents the signature of a routine overload. This is distinct from {@link RoutineSignature}, as it is specific
 * to the overload, and not the instance. The overload signature is used to determine if a routine overload is applicable
 * to a given call site, and if so, which routine overload to use.
 * </p>
 * <p>
 * This differs from {@link RoutineSignature}, as it does not have {@link RoutineSignature#isNullCall()} and
 * {@link RoutineSignature#isMissingCall()}, among others.
 * </p>
 */
public final class RoutineOverloadSignature {
    @NotNull
    private final String name;
    @NotNull
    private final List<PType> paramTypes;

    /**
     * Creates a new {@link RoutineOverloadSignature} with the given name and parameters.
     * Parameter-type metadata is recursively snapshotted. Supported metadata values are {@code null}, strings, boxed
     * primitive values, lists, sets, maps, and arrays composed from those values. Unsupported values and cyclic
     * containers are rejected. A reference array is widened to {@code Object[]} when a copied element is not assignable
     * to its original component type.
     *
     * @param name the name of the function
     * @param parameterTypes the types of the parameters of the function
     * @throws NullPointerException if the name, parameter-type list, or one of its elements is null
     * @throws IllegalArgumentException if parameter metadata contains an unsupported value or a cycle
     */
    public RoutineOverloadSignature(@NotNull String name, @NotNull List<PType> parameterTypes) {
        this.name = Objects.requireNonNull(name, "name");
        this.paramTypes = RoutineParameterTypes.snapshot(parameterTypes);
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
     * Returns the preferred types of the parameters of the function. This is used for the sorting of {@link FnOverload}
     * and {@link AggOverload}.
     * @return the preferred types of the parameters of the function
     */
    public List<PType> getParameterTypes() {
        return RoutineParameterTypes.snapshot(paramTypes);
    }
}
