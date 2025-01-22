package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.spi.types.PType;
import org.partiql.spi.value.Datum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * This provides potentially overloaded implementations of a particular scalar function (e.g. {@code ABS(int)},
 * {@code ABS(decimal(p, s))}, etc.).
 * </p>
 * @see Builder
 * @see Fn
 */
public abstract class FnProvider {

    /**
     * Returns the signature of the function provider.
     * @return the signature of the function provider.
     */
    @NotNull
    public abstract RoutineProviderSignature getSignature();

    /**
     * Returns an instance of the function for the given argument types.
     * @param args the argument types.
     * @return an instance of the function for the given argument types, or {@code null} if no such function exists.
     */
    @Nullable
    public abstract Fn getInstance(PType[] args);

    /**
     * A simple builder for {@link FnProvider} that provides a single implementation of a scalar function. This
     * does not handle overloads.
     * @see FnProvider
     */
    public static class Builder {

        @NotNull
        private final String name;
        private final List<Parameter> parameters = new ArrayList<>();
        private PType returns = PType.dynamic();
        private Function<Datum[], Datum> invocation;
        private boolean isNullCall = true;
        private boolean isMissingCall = true;

        /**
         * Creates a new {@link Builder} for a {@link FnProvider} with the given name.
         * @param name the name of the function.
         */
        public Builder(@NotNull String name) {
            this.name = name;
        }

        /**
         * Adds a {@link Parameter} to the {@link Fn}.
         * @param param the {@link Parameter} to add.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder addParameter(@NotNull Parameter param) {
            this.parameters.add(param);
            return this;
        }

        /**
         * Adds {@link Parameter}s to the {@link Fn}.
         * @param parameters the {@link Parameter} to add.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder addParameters(@NotNull Parameter... parameters) {
            this.parameters.addAll(Arrays.asList(parameters));
            return this;
        }

        /**
         * Adds {@link Parameter}s to the {@link Fn}.
         * @param parameters the {@link Parameter} to add.
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
         * @return The {@link Builder} instance.
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
        public Builder addParameter(@NotNull PType type) {
            Parameter param = new Parameter("arg" + parameters.size(), type);
            this.parameters.add(param);
            return this;
        }

        /**
         * Specifies whether the function should be called when the argument is null.
         * @param value if true, the function should not be invoked, and null shall be returned; if false, the function
         *              shall be invoked, and implementations of the function should handle this scenario.
         * @return the {@link Builder} instance.
         */
        @NotNull
        public Builder isNullCall(boolean value) {
            this.isNullCall = value;
            return this;
        }

        /**
         * Specifies whether the function should be called when the argument is missing.
         * @param value if true, the function should not be invoked, and missing shall be returned; if false, the function
         *              shall be invoked, and implementations of the function should handle this scenario.
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
         * Builds the {@link FnProvider}.
         * @return the {@link FnProvider} instance.
         */
        @NotNull
        public FnProvider build() {
            List<RoutineProviderParameter> providerParameters = parameters.stream().map(
                    p -> new RoutineProviderParameter(p.getName(), p.getType())
            ).collect(Collectors.toList());
            RoutineProviderSignature pSignature = new RoutineProviderSignature(name, providerParameters);
            Fn instance = new Fn.Builder(name)
                    .returns(returns)
                    .addParameters(parameters)
                    .body(invocation)
                    .isNullCall(isNullCall)
                    .isMissingCall(isMissingCall)
                    .build();
            return new FnProviderImpl(pSignature, instance);
        }
    }

    private static class FnProviderImpl extends FnProvider {

        @NotNull
        private final RoutineProviderSignature signature;

        @NotNull
        private final Fn instance;

        public FnProviderImpl(@NotNull RoutineProviderSignature signature, @NotNull Fn instance) {
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
        public Fn getInstance(PType[] args) {
            return instance;
        }
    }
}
