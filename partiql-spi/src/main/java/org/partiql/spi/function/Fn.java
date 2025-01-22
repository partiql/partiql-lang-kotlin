package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;
import org.partiql.spi.value.Datum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * <p>
 * Represents a scalar function and its implementation.
 * </p>
 * <p>
 * This differs from {@link FnProvider} because {@link Fn} represents an implementation of a single scalar function with
 * a particular {@link RoutineSignature}, whereas {@link FnProvider} delegates to 1 or more (if overloaded) {@link Fn}s
 * of a particular arity.
 * </p>
 * <p>
 * As an example, {@link Fn} may hold the implementation for {@code ABS(int) -> int}; however {@link FnProvider}
 * may reference all overloads of {@code ABS(x)} (1 parameter), including the implementations ({@link Fn}s) of
 * {@code ABS(int) -> int} (from before) as well as {@code ABS(float) -> float}, {@code ABS(double) -> double}, and any
 * others.
 * </p>
 * @see FnProvider.Builder
 * @see FnProvider
 * @see Builder
 */
public abstract class Fn {

    /**
     * Returns the signature of the function.
     * @return the signature of the function.
     */
    @NotNull
    public abstract RoutineSignature getSignature();

    /**
     * Computes the result of the function.
     * @param args the arguments to the function.
     * @return the result of the function.
     */
    @NotNull
    public abstract Datum invoke(Datum[] args);

    /**
     * A builder for creating {@link Fn}s.
     * @see FnProvider.Builder
     */
    public static final class Builder {

        private final List<Parameter> parameters = new ArrayList<>();
        private final String name;
        private PType returns = PType.dynamic();
        private Function<Datum[], Datum> invocation;
        private boolean isNullCall = true;
        private boolean isMissingCall = true;

        /**
         * Creates a new {@link Builder} for a {@link Fn} with the given name.
         * @param name the name of the function.
         */
        public Builder(@NotNull String name) {
            this.name = name;
        }

        /**
         * Adds a {@link Parameter} to the {@link Fn}.
         * @param param the parameter to add.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder addParameter(@NotNull Parameter param) {
            this.parameters.add(param);
            return this;
        }

        /**
         * Adds {@link Parameter}s to the {@link Fn}.
         * @param parameters the parameters to add.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder addParameters(@NotNull Parameter... parameters) {
            this.parameters.addAll(Arrays.asList(parameters));
            return this;
        }

        /**
         * Adds {@link Parameter}s to the {@link Fn}.
         * @param parameters the parameters to add.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder addParameters(@NotNull List<Parameter> parameters) {
            this.parameters.addAll(parameters);
            return this;
        }

        /**
         * Adds {@link Parameter}s to the {@link Fn} with the given {@link PType}s. The name of the {@link
         * Parameter}s are automatically generated.
         * @param types the types of the parameters
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder addParameters(@NotNull PType... types) {
            for (PType type : types) {
                addParameter(type);
            }
            return this;
        }

        /**
         * Adds a {@link Parameter} to the {@link Fn} with the given {@link PType}. The name of the {@link Parameter} is
         * automatically generated.
         * @param type the type of the parameter.
         * @return the {@link Builder} instance.
         */
        @NotNull
        @SuppressWarnings("UnusedReturnValue")
        public Builder addParameter(@NotNull PType type) {
            Parameter param = new Parameter("arg" + parameters.size(), type);
            return this.addParameter(param);
        }

        /**
         * Sets the IS NULL CALL status.
         * @param value the IS NULL CALL status.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder isNullCall(boolean value) {
            this.isNullCall = value;
            return this;
        }

        /**
         * Sets the IS MISSING CALL status.
         * @param value the IS MISSING CALL status.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder isMissingCall(boolean value) {
            this.isMissingCall = value;
            return this;
        }

        /**
         * Sets the return type of the {@link Fn}.
         * @param returns the return type of the {@link Fn}.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder returns(@NotNull PType returns) {
            this.returns = returns;
            return this;
        }

        /**
         * Sets the implementation of the function.
         * @param impl the implementation of the function.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder body(@NotNull Function<Datum[], Datum> impl) {
            this.invocation = impl;
            return this;
        }

        /**
         * Builds the {@link Fn}.
         * @return the {@link Fn} instance.
         */
        @NotNull
        public Fn build() {
            return new FnImpl(
                    name,
                    returns,
                    parameters,
                    invocation,
                    isNullCall,
                    isMissingCall
            );
        }
    }

    private static class FnImpl extends Fn {

        @NotNull
        private final RoutineSignature signature;

        @NotNull
        private final Function<Datum[], Datum> implementation;

        public FnImpl(
                @NotNull String name,
                @NotNull PType returns,
                @NotNull List<Parameter> parameters,
                @NotNull Function<Datum[], Datum> invocation,
                boolean isNullCall,
                boolean isMissingCall
        ) {
            this.signature = new RoutineSignature(name, parameters, returns, isNullCall, isMissingCall);
            this.implementation = invocation;
        }

        @NotNull
        @Override
        public RoutineSignature getSignature() {
            return signature;
        }

        @NotNull
        @Override
        public Datum invoke(Datum[] args) {
            return implementation.apply(args);
        }
    }
}
