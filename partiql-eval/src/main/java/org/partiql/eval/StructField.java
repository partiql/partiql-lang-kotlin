package org.partiql.eval;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the key-value pairs that are embedded within values of type {@link org.partiql.value.PartiQLValueType#STRUCT}.
 */
public interface StructField {

    /**
     * @return the key in the key-value pair that the {@link StructField} represents.
     */
    @NotNull
    String getName();

    /**
     * @return the value in the key-value pair that the {@link StructField} represents.
     */
    @NotNull
    PQLValue getValue();

    /**
     * Returns an instance of a {@link StructField}
     * @param name the key in the key-value pair
     * @param value the value in the key-value pair
     * @return an instance of {@link StructField} that holds the {@code name} and {@code value}
     */
    @NotNull
    static StructField of(@NotNull String name, @NotNull PQLValue value) {
        return new StructField() {
            @NotNull
            @Override
            public String getName() {
                return name;
            }

            @NotNull
            @Override
            public PQLValue getValue() {
                return value;
            }
        };
    }
}
