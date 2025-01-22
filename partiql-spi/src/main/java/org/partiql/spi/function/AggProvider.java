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
 * This provides implementations of a specific aggregation function (e.g. SUM, MIN, MAX, etc.) for a given
 * {@link RoutineProviderSignature}. This API can be leveraged to represent multiple implementations of the same
 * function, e.g. {@code SUM(int)}, {@code SUM(smallint)}, {@code SUM(decimal(p, s))}, etc.
 * </p>
 * @see Builder
 * @see Agg
 * @see RoutineProviderSignature
 */
public abstract class AggProvider {

    /**
     * Returns the signature of this {@link AggProvider}.
     * @return the signature of this {@link AggProvider}.
     */
    @NotNull
    public abstract RoutineProviderSignature getSignature();

    /**
     * Retrieves an instance of {@link Agg} for the given arguments. The {@link Agg}'s parameters' types may not match
     * the arguments' types.
     * @param args the arguments' types to the {@link Agg}.
     * @return an instance of {@link Agg} for the given arguments; null if no such instance exists.
     */
    @Nullable
    public abstract Agg getInstance(PType[] args);

    /**
     * A simple builder for {@link AggProvider} that provides a single implementation of an aggregation function. This
     * does not handle overloads.
     * @see AggProvider
     */
    public static class Builder {
        @NotNull
        private final String name;

        @NotNull
        private final List<Parameter> parameters = new ArrayList<>();

        @NotNull
        private PType returns = PType.dynamic();

        @NotNull
        private Callable<Accumulator> body = () -> null;

        /**
         * Creates a new {@link Builder} for an {@link AggProvider}.
         * @param name the name of the function.
         */
        public Builder(@NotNull String name) {
            this.name = name;
        }

        /**
         * Adds a {@link Parameter} to the {@link AggProvider} with the given {@link PType}. The name of the {@link
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
         * Adds multiple {@link Parameter}s to the {@link AggProvider} with the given {@link PType}s. The names of the {@link
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
         * Sets the return type of the {@link AggProvider#getInstance(PType[])}.
         * @param type the return type of the {@link AggProvider#getInstance(PType[])}.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder returns(@NotNull PType type) {
            this.returns = type;
            return this;
        }

        /**
         * Sets the {@link Accumulator} provider for the {@link AggProvider}.
         * @param body the {@link Accumulator} provider for the {@link AggProvider}.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder body(@NotNull Callable<Accumulator> body) {
            this.body = body;
            return this;
        }

        /**
         * Builds the {@link AggProvider}.
         * @return the {@link AggProvider} instance.
         */
        @NotNull
        public AggProvider build() {
            List<RoutineProviderParameter> providerParameters = parameters.stream().map(
                    p -> new RoutineProviderParameter(p.getName(), p.getType())
            ).collect(Collectors.toList());
            RoutineProviderSignature signature = new RoutineProviderSignature(name, providerParameters);
            RoutineSignature routineSignature = new RoutineSignature(name, parameters, returns);
            Agg instance = new AggImpl(body, routineSignature);
            return new AggProviderImpl(signature, instance);
        }
    }

    private static class AggProviderImpl extends AggProvider {
        @NotNull
        private final RoutineProviderSignature signature;

        @NotNull
        private final Agg instance;

        public AggProviderImpl(@NotNull RoutineProviderSignature signature, @NotNull Agg instance) {
            this.signature = signature;
            this.instance = instance;
        }

        @NotNull
        @Override
        public RoutineProviderSignature getSignature() {
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
