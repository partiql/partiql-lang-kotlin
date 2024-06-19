package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumChars implements Datum {

    @NotNull
    private final String _value;

    DatumChars(@NotNull String value) {
        _value = value;
    }

    @Override
    @NotNull
    public String getString() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return PType.typeChar(255); // TODO: Figure out max length
    }
}
