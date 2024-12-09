package org.partiql.types;

class PTypeVariant extends PType {

    // TODO: Use this somehow
    private final String encoding;

    public PTypeVariant(String encoding) {
        super(VARIANT);
        this.encoding = encoding;
    }
}
