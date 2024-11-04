package org.partiql.types;

import org.jetbrains.annotations.NotNull;

public class PTypeVariant implements PType {

    private final String encoding;

    public PTypeVariant(String encoding) {
        this.encoding = encoding;
    }

    @NotNull
    @Override
    public Kind getKind() {
        return Kind.VARIANT;
    }

}
