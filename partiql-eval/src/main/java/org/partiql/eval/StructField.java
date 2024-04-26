package org.partiql.eval;

import org.jetbrains.annotations.NotNull;

public interface StructField {
    @NotNull
    String getName();

    @NotNull
    PQLValue getValue();

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
