package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a field within a PartiQL {@link org.partiql.value.PartiQLValueType#STRUCT}.
 */
public interface Field {

    /**
     * @return the key in the key-value pair that the {@link Field} represents.
     */
    @NotNull
    String getName();

    /**
     * @return the value in the key-value pair that the {@link Field} represents.
     */
    @NotNull
    Datum getValue();

    /**
     * Returns an instance of a {@link Field}
     * @param name the key in the key-value pair
     * @param value the value in the key-value pair
     * @return an instance of {@link Field} that holds the {@code name} and {@code value}
     */
    @NotNull
    static Field of(@NotNull String name, @NotNull Datum value) {
        return new Field() {
            @NotNull
            @Override
            public String getName() {
                return name;
            }

            @NotNull
            @Override
            public Datum getValue() {
                return value;
            }
        };
    }
}
