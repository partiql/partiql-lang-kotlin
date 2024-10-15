package org.partiql.spi;

import java.util.Objects;

/**
 * Represents a location in a PartiQL statement.
 */
public class SourceLocation {
    // TODO: Should we declare all line & offset to start at 0 or 1?

    /**
     * The line number of the location indicated.
     */
    public long line;

    /**
     * The offset of the location indicated.
     */
    public long offset;

    /**
     * The length of the location indicated.
     */
    public long length;

    // Private to not allow for the creation of this class outside the proper constructor.
    private SourceLocation() {
        line = 0;
        offset = 0;
        length = 0;
    }

    public SourceLocation(long line, long offset, long length) {
        this.line = line;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SourceLocation)) return false;
        SourceLocation that = (SourceLocation) o;
        return line == that.line && offset == that.offset && length == that.length;
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, offset, length);
    }
}
