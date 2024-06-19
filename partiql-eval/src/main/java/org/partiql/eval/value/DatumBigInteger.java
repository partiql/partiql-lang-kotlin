package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.PartiQLValueType;

import java.math.BigInteger;

/**
 * This shall always be package-private (internal).
 */
class DatumBigInteger implements Datum {

    @NotNull
    private final BigInteger _value;

    private final static PType _type = PType.typeIntArbitrary();

    DatumBigInteger(@NotNull BigInteger value) {
        _value = value;
    }

    @Override
    @NotNull
    public BigInteger getBigInteger() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }
}
