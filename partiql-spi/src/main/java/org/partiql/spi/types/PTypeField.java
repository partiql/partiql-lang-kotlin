package org.partiql.spi.types;

import org.jetbrains.annotations.NotNull;

/**
 * This represents a field of a structured type.
 */
public interface PTypeField {
    @NotNull
    public String getName();

    @NotNull
    public PType getType();


    /**
     * Returns a simple implementation of {@link PTypeField}.
     * @param name the key of the struct field
     * @param type the type of the struct field
     * @return a field containing the name and type
     */
    static PTypeField of(@NotNull String name, @NotNull PType type) {
        return new PTypeField() {
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
                if (!(o instanceof PTypeField)) return false;
                return name.equals(((PTypeField) o).getName()) && type.equals(((PTypeField) o).getType());
            }
        };
    }
}
