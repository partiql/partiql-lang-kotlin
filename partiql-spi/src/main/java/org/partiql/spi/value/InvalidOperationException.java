package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * Represents an invalid operation on a {@link Datum}.
 * @see Datum
 */
public final class InvalidOperationException extends RuntimeException {

    /**
     * This should not be used for production purposes, as the returned string is subject to change. This should only
     * be used for debugging.
     * @return the details message of this exception.
     */
    @Override
    public String getMessage() {
        return super.getMessage();
    }

    /**
     * @param type the type of the {@link Datum} that does not support the operation
     * @param operation the offending operation
     */
    public InvalidOperationException(@NotNull PType type, @NotNull String operation) {
        super("Operation \"" + operation + "\" is not valid for type " + type + ".");
    }
}
