package org.partiql.types;

import org.jetbrains.annotations.NotNull;

/**
 * This represents a field of a structured type.
 */
public interface Field {
    @NotNull
    public String getName();

    @NotNull
    public PType getType();


    /**
     * Returns a simple implementation of {@link Field}.
     * @param name the key of the struct field
     * @param type the type of the struct field
     * @return a field containing the name and type
     */
    static Field of(@NotNull String name, @NotNull PType type) {
        return new Field() {
            @NotNull
            @Override
            public String getName() {
                return name;
            }

            @NotNull
            @Override
            public PType getType() {
                return type;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Field)) return false;
                return name.equals(((Field) o).getName()) && type.equals(((Field) o).getType());
            }
        };
    }
}