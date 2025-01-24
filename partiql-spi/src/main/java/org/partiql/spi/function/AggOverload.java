package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.spi.types.PType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * <p>
 * This provides potentially overloaded implementations of a particular aggregation function (e.g. SUM, MIN, MAX, etc.) for a given
 * {@link RoutineOverloadSignature}. This API can be leveraged to represent multiple implementations of the same
 * function of the same arity, e.g. {@code SUM(int)}, {@code SUM(smallint)}, {@code SUM(decimal(p, s))}, etc.
 * </p>
 * @see Builder
 * @see Agg
 * @see RoutineOverloadSignature
 */
public abstract class AggOverload {

    /**
     * Returns the signature of this {@link AggOverload}.
     * @return the signature of this {@link AggOverload}.
     */
    @NotNull
    public abstract RoutineOverloadSignature getSignature();

    /**
     * Retrieves an instance of {@link Agg} for the given arguments. The {@link Agg}'s parameters' types may not match
     * the arguments' types.
     * @param args the arguments' types to the {@link Agg}.
     * @return an instance of {@link Agg} for the given arguments; null if no such instance exists.
     */
    @Nullable
    public abstract Agg getInstance(PType[] args);

    /**
     * A simple builder for {@link AggOverload} that provides a single implementation of an aggregation function. This
     * does not handle overloads.
     * @see AggOverload
     */
    public static final class Builder {
        @NotNull
        private final String name;

        @NotNull
        private final List<Parameter> parameters = new ArrayList<>();

        @NotNull
        private PType returns = PType.dynamic();

        @NotNull
        private Callable<Accumulator> body = () -> null;

        /**
         * Creates a new {@link Builder} for an {@link AggOverload}.
         * @param name the name of the function.
         */
        public Builder(@NotNull String name) {
            this.name = name;
        }

        /**
         * Adds a {@link Parameter} to the {@link AggOverload} with the given {@link PType}. The name of the {@link
         * Parameter} is automatically generated.
         * @param type The type of the parameter.
         * @return the {@link Builder} instance.
         */
        @SuppressWarnings("UnusedReturnValue")
        @NotNull
        public Builder addParameter(@NotNull PType type) {
            this.parameters.add(new Parameter("arg" + parameters.size(), type));
            return this;
        }

        /**
         * Adds multiple {@link Parameter}s to the {@link AggOverload} with the given {@link PType}s. The names of the {@link
         * Parameter}s are automatically generated.
         * @param types The types of the parameters.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder addParameters(@NotNull PType... types) {
            for (PType type : types) {
                this.addParameter(type);
            }
            return this;
        }

        /**
         * Sets the return type of the {@link AggOverload#getInstance(PType[])}.
         * @param type the return type of the {@link AggOverload#getInstance(PType[])}.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder returns(@NotNull PType type) {
            this.returns = type;
            return this;
        }

        /**
         * Sets the {@link Accumulator} provider for the {@link AggOverload}.
         * @param body the {@link Accumulator} provider for the {@link AggOverload}.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder body(@NotNull Callable<Accumulator> body) {
            this.body = body;
            return this;
        }

        /**
         * Builds the {@link AggOverload}.
         * @return the {@link AggOverload} instance.
         */
        @NotNull
        public AggOverload build() {
            List<PType> parameterTypes = parameters.stream().map(Parameter::getType).collect(Collectors.toList());
            RoutineOverloadSignature signature = new RoutineOverloadSignature(name, parameterTypes);
            RoutineSignature routineSignature = new RoutineSignature(name, parameters, returns);
            Agg instance = new AggImpl(body, routineSignature);
            return new AggOverloadImpl(signature, instance);
        }
    }

    private static class AggOverloadImpl extends AggOverload {
        @NotNull
        private final RoutineOverloadSignature signature;

        @NotNull
        private final Agg instance;

        public AggOverloadImpl(@NotNull RoutineOverloadSignature signature, @NotNull Agg instance) {
            this.signature = signature;
            this.instance = instance;
        }

        @NotNull
        @Override
        public RoutineOverloadSignature getSignature() {
            return signature;
        }

        @Nullable
        @Override
        public Agg getInstance(PType[] args) {
            return instance;
        }
    }

    private static class AggImpl extends Agg {
        @NotNull
        private final Callable<Accumulator> accumulator;

        @NotNull
        private final RoutineSignature signature;

        public AggImpl(@NotNull Callable<Accumulator> accumulator, @NotNull RoutineSignature signature) {
            this.accumulator = accumulator;
            this.signature = signature;
        }

        @NotNull
        @Override
        public Accumulator getAccumulator() {
            try {
                return accumulator.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @NotNull
        @Override
        public RoutineSignature getSignature() {
            return signature;
        }
    }
}
