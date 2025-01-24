package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * Represents an invocable routine's signature.
 * @see Agg
 * @see Fn
 */
public final class RoutineSignature {
    @NotNull
    private final String name;
    @NotNull
    private final List<Parameter> params;
    @NotNull
    private final PType returns;
    private final boolean isNullCall;
    private final boolean isMissingCall;

    /**
     * Creates a routine signature, whose IS NULL/MISSING call status is set to true.
     * @param name the name of the routine.
     * @param params the parameters of the routine.
     * @param returns the return type of the routine.
     */
    public RoutineSignature(@NotNull String name, @NotNull List<Parameter> params, @NotNull PType returns) {
        this.name = name;
        this.params = params;
        this.returns = returns;
        this.isNullCall = true;
        this.isMissingCall = true;
    }

    /**
     * Creates a routine signature.
     * @param name the name of the routine.
     * @param params the parameters of the routine.
     * @param returns the return type of the routine.
     * @param isNullCall the status of the IS NULL CALL for the routine.
     * @param isMissingCall the status of the MISSING CALL for the routine.
     */
    public RoutineSignature(@NotNull String name, @NotNull List<Parameter> params, @NotNull PType returns, boolean isNullCall, boolean isMissingCall) {
        this.name = name;
        this.params = params;
        this.returns = returns;
        this.isNullCall = isNullCall;
        this.isMissingCall = isMissingCall;
    }

    /**
     * Returns the name of the routine.
     * @return the name of the routine.
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the return type of the routine.
     * @return the return type of the routine.
     */
    @NotNull
    public PType getReturns() {
        return returns;
    }

    /**
     * Returns the arity (number of parameters) of the routine.
     * @return the arity (number of parameters) of the routine.
     */
    public int getArity() {
        return getParameters().size();
    }

    /**
     * Returns the parameters of the routine.
     * @return the parameters of the routine.
     */
    @NotNull
    public List<Parameter> getParameters() {
        return params;
    }

    /**
     * Returns the IS MISSING CALL status of the routine.
     * @return the IS MISSING CALL status of the routine.
     */
    public boolean isMissingCall() {
        return isMissingCall;
    }

    /**
     * Returns the IS NULL CALL status of the routine.
     * @return the IS NULL CALL status of the routine.
     */
    public boolean isNullCall() {
        return isNullCall;
    }
}
